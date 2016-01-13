package org.molgenis.data.mapper.service.impl;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.mapper.meta.MappingProjectMetaData.NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Lists;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Repository;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.mapping.model.MappingTarget;
import org.molgenis.data.mapper.repository.MappingProjectRepository;
import org.molgenis.data.mapper.service.AlgorithmService;
import org.molgenis.data.mapper.service.MappingService;
import org.molgenis.data.meta.PackageImpl;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.fieldtypes.FieldType;
import org.molgenis.security.core.runas.RunAsSystem;
import org.molgenis.security.permission.PermissionSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;

public class MappingServiceImpl implements MappingService
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingServiceImpl.class);

	private final DataService dataService;

	private final AlgorithmService algorithmService;

	private final IdGenerator idGenerator;

	private final MappingProjectRepository mappingProjectRepository;

	private final PermissionSystemService permissionSystemService;

	@Autowired
	public MappingServiceImpl(DataService dataService, AlgorithmService algorithmService, IdGenerator idGenerator,
			MappingProjectRepository mappingProjectRepository, PermissionSystemService permissionSystemService)
	{
		this.dataService = requireNonNull(dataService);
		this.algorithmService = requireNonNull(algorithmService);
		this.idGenerator = requireNonNull(idGenerator);
		this.mappingProjectRepository = requireNonNull(mappingProjectRepository);
		this.permissionSystemService = requireNonNull(permissionSystemService);
	}

	@Override
	@RunAsSystem
	public MappingProject addMappingProject(String projectName, MolgenisUser owner, String target)
	{
		MappingProject mappingProject = new MappingProject(projectName, owner);
		mappingProject.addTarget(dataService.getEntityMetaData(target));
		mappingProjectRepository.add(mappingProject);
		return mappingProject;
	}

	@Override
	@RunAsSystem
	public void deleteMappingProject(String mappingProjectId)
	{
		mappingProjectRepository.delete(mappingProjectId);
	}

	@Override
	@PreAuthorize("hasAnyRole('ROLE_SYSTEM, ROLE_SU, ROLE_PLUGIN_WRITE_MENUMANAGER')")
	@Transactional
	public MappingProject cloneMappingProject(String mappingProjectId)
	{
		MappingProject mappingProject = mappingProjectRepository.getMappingProject(mappingProjectId);
		if (mappingProject == null)
		{
			throw new UnknownEntityException("Mapping project [" + mappingProjectId + "] does not exist");
		}
		String mappingProjectName = mappingProject.getName();

		// determine cloned mapping project name (use Windows 7 naming strategy):
		String clonedMappingProjectName;
		for (int i = 1;; ++i)
		{
			if (i == 1)
			{
				clonedMappingProjectName = mappingProjectName + " - Copy";
			}
			else
			{
				clonedMappingProjectName = mappingProjectName + " - Copy (" + i + ")";
			}

			if (mappingProjectRepository.getMappingProjects(new QueryImpl().eq(NAME, clonedMappingProjectName))
					.isEmpty())
			{
				break;
			}
		}

		return cloneMappingProject(mappingProject, clonedMappingProjectName);
	}

	@Override
	@PreAuthorize("hasAnyRole('ROLE_SYSTEM, ROLE_SU, ROLE_PLUGIN_WRITE_MENUMANAGER')")
	@Transactional
	public MappingProject cloneMappingProject(String mappingProjectId, String clonedMappingProjectName)
	{
		MappingProject mappingProject = mappingProjectRepository.getMappingProject(mappingProjectId);
		if (mappingProject == null)
		{
			throw new UnknownEntityException("Mapping project [" + mappingProjectId + "] does not exist");
		}

		return cloneMappingProject(mappingProject, clonedMappingProjectName);
	}

	private MappingProject cloneMappingProject(MappingProject mappingProject, String clonedMappingProjectName)
	{
		mappingProject.removeIdentifiers();
		mappingProject.setName(clonedMappingProjectName);
		mappingProjectRepository.add(mappingProject);
		return mappingProject;
	}

	@Override
	@RunAsSystem
	public List<MappingProject> getAllMappingProjects()
	{
		return mappingProjectRepository.getAllMappingProjects();
	}

	@Override
	@RunAsSystem
	public void updateMappingProject(MappingProject mappingProject)
	{
		mappingProjectRepository.update(mappingProject);
	}

	@Override
	@RunAsSystem
	public MappingProject getMappingProject(String identifier)
	{
		return mappingProjectRepository.getMappingProject(identifier);
	}

	@Override
	public String applyMappings(MappingTarget mappingTarget, String entityName)
	{
		DefaultEntityMetaData targetMetaData = new DefaultEntityMetaData(entityName, mappingTarget.getTarget());
		targetMetaData.setPackage(PackageImpl.defaultPackage);
		targetMetaData.setLabel(entityName);
		targetMetaData.addAttribute("source");

		Repository targetRepo;
		if (!dataService.hasRepository(entityName))
		{
			targetRepo = dataService.getMeta().addEntityMeta(targetMetaData);
			permissionSystemService.giveUserEntityPermissions(SecurityContextHolder.getContext(),
					Collections.singletonList(targetRepo.getName()));
		}
		else
		{
			targetRepo = dataService.getRepository(entityName);

			if (!isTargetMetaCompatible(targetRepo, targetMetaData))
			{
				throw new MolgenisDataException(
						"Target entity " + entityName + " exists but is not compatible with the target mappings.");
			}
		}

		try
		{
			applyMappingsToRepositories(mappingTarget, targetRepo);
			return targetMetaData.getName();
		}
		catch (RuntimeException ex)
		{
			LOG.error("Error applying mappings, dropping created repository.", ex);
			dataService.getMeta().deleteEntityMeta(targetMetaData.getName());
			throw ex;
		}
	}

	private boolean isTargetMetaCompatible(Repository targetRepo, EntityMetaData mappingTargetMetaData)
	{
		Map<String, AttributeMetaData> targetRepoAttributeMap = Maps.newHashMap();
		targetRepo.getEntityMetaData().getAtomicAttributes()
				.forEach(attr -> targetRepoAttributeMap.put(attr.getName(), attr));

		for (AttributeMetaData mappingTargetAttr : mappingTargetMetaData.getAtomicAttributes())
		{
			String mappingTargetAttrName = mappingTargetAttr.getName();
			if (targetRepoAttributeMap.containsKey(mappingTargetAttrName)
					&& targetRepoAttributeMap.get(mappingTargetAttrName).isSameAs(mappingTargetAttr))
			{
				continue;

			}
			else
			{
				return false;
			}
		}
		return true;
	}

	private void applyMappingsToRepositories(MappingTarget mappingTarget, Repository targetRepo)
	{
		// collect (mapped) ids from all sources to keep track of deleted entities
		List<Object> targetRepoIds = Lists.newArrayList();
		List<String> mappedSourceIds = Lists.newArrayList();

		targetRepo.forEach(entity -> targetRepoIds.add(entity.getIdValue().toString()));

		for (EntityMapping sourceMapping : mappingTarget.getEntityMappings())
		{
			applyMappingToRepo(sourceMapping, targetRepo, mappedSourceIds);
		}

		targetRepoIds.removeAll(mappedSourceIds);
		if (!targetRepoIds.isEmpty())
		{
			targetRepo.deleteById(targetRepoIds);
		}

	}

	private void applyMappingToRepo(EntityMapping sourceMapping, Repository targetRepo, List<String> mappedSourceIds)
	{
		EntityMetaData targetMetaData = targetRepo.getEntityMetaData();
		Repository sourceRepo = dataService.getRepository(sourceMapping.getName());
		for (Entity sourceEntity : sourceRepo)
		{
			MapEntity mappedEntity = applyMappingToEntity(sourceMapping, sourceEntity, targetMetaData,
					sourceMapping.getSourceEntityMetaData(), targetRepo);

			mappedSourceIds.add(mappedEntity.getIdValue().toString());

			if (targetRepo.findOne(mappedEntity.getIdValue()) != null)
			{
				targetRepo.update(mappedEntity);
			}
			else
			{
				targetRepo.add(mappedEntity);
			}
		}
	}

	private MapEntity applyMappingToEntity(EntityMapping sourceMapping, Entity sourceEntity,
			EntityMetaData targetMetaData, EntityMetaData sourceEntityMetaData, Repository targetRepository)
	{
		MapEntity target = new MapEntity(targetMetaData);
		target.set("source", sourceMapping.getName());

		sourceMapping.getAttributeMappings().forEach(attributeMapping -> applyMappingToAttribute(attributeMapping,
				sourceEntity, target, sourceEntityMetaData));
		return target;
	}

	@Override
	public String generateId(FieldType dataType, Long count)
	{
		Object id;
		if (dataType.equals(MolgenisFieldTypes.INT) || dataType.equals(MolgenisFieldTypes.LONG)
				|| dataType.equals(MolgenisFieldTypes.DECIMAL))
		{
			id = count + 1;
		}
		else
		{
			id = idGenerator.generateId();
		}
		return id.toString();
	}

	private void applyMappingToAttribute(AttributeMapping attributeMapping, Entity sourceEntity, MapEntity target,
			EntityMetaData entityMetaData)
	{
		// TODO: skip id when id is AUTO
		target.set(attributeMapping.getTargetAttributeMetaData().getName(),
				algorithmService.apply(attributeMapping, sourceEntity, entityMetaData));
	}
}
