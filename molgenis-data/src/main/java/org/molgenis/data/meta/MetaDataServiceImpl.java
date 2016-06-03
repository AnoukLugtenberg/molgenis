package org.molgenis.data.meta;

import static com.google.common.collect.Lists.reverse;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.ATTRIBUTE_META_DATA;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ENTITY_META_DATA;
import static org.molgenis.data.meta.PackageMetaData.PACKAGE;
import static org.molgenis.data.meta.PackageMetaData.PARENT;
import static org.molgenis.data.meta.TagMetaData.TAG;
import static org.molgenis.util.SecurityDecoratorUtils.validatePermission;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.molgenis.data.Entity;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.RepositoryCollectionRegistry;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.meta.system.SystemEntityMetaDataRegistry;
import org.molgenis.data.support.DataServiceImpl;
import org.molgenis.fieldtypes.CompoundField;
import org.molgenis.security.core.Permission;
import org.molgenis.util.DependencyResolver;
import org.molgenis.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeTraverser;

/**
 * TODO add docs
 */
public class MetaDataServiceImpl implements MetaDataService
{
	private static final Logger LOG = LoggerFactory.getLogger(MetaDataServiceImpl.class);

	private final DataServiceImpl dataService;
	private RepositoryCollectionRegistry repositoryCollectionRegistry;
	private RepositoryCollection defaultBackend;
	private SystemEntityMetaDataRegistry systemEntityMetaDataRegistry;
	private PackageMetaData packageMetaData;
	private EntityMetaDataMetaData entityMetaDataMetaData;
	private AttributeMetaDataMetaData attributeMetaDataMetaData;
	private TagMetaData tagMetaData;

	public MetaDataServiceImpl(DataServiceImpl dataService)
	{
		this.dataService = requireNonNull(dataService);
	}

	@Autowired
	public void setSystemEntityMetaDataRegistry(SystemEntityMetaDataRegistry systemEntityMetaDataRegistry)
	{
		this.systemEntityMetaDataRegistry = requireNonNull(systemEntityMetaDataRegistry);
	}

	@Autowired
	public void setRepositoryCollectionRegistry(RepositoryCollectionRegistry repositoryCollectionRegistry)
	{
		this.repositoryCollectionRegistry = requireNonNull(repositoryCollectionRegistry);
	}

	@Autowired
	public void setPackageMetaData(PackageMetaData packageMetaData)
	{
		this.packageMetaData = requireNonNull(packageMetaData);
	}

	@Autowired
	public void setEntityMetaDataMetaData(EntityMetaDataMetaData entityMetaDataMetaData)
	{
		this.entityMetaDataMetaData = requireNonNull(entityMetaDataMetaData);
	}

	@Autowired
	public void setAttributeMetaDataMetaData(AttributeMetaDataMetaData attributeMetaDataMetaData)
	{
		this.attributeMetaDataMetaData = requireNonNull(attributeMetaDataMetaData);
	}

	@Autowired
	public void setTagMetaData(TagMetaData tagMetaData)
	{
		this.tagMetaData = requireNonNull(tagMetaData);
	}

	@Override
	public Repository getRepository(String entityName)
	{
		EntityMetaData entityMeta = getEntityMetaData(entityName);
		if (entityMeta == null)
		{
			throw new UnknownEntityException(format("Unknown entity [%s]", entityName));
		}
		return getRepository(entityMeta);
	}

	@Override
	public Repository getRepository(EntityMetaData entityMeta)
	{
		String backendName = entityMeta.getBackend();
		RepositoryCollection backend = getBackend(backendName);
		return backend.getRepository(entityMeta);
	}

	@Override
	public MetaDataService setDefaultBackend(RepositoryCollection backend)
	{
		this.defaultBackend = backend;
		return this;
	}

	@Override
	public RepositoryCollection getDefaultBackend()
	{
		return repositoryCollectionRegistry.getDefaultRepoCollection();
	}

	@Override
	public RepositoryCollection getBackend(String name)
	{
		return repositoryCollectionRegistry.getRepositoryCollection(name);
	}

	/**
	 * Removes entity meta data if it exists.
	 */
	@Override
	public void deleteEntityMeta(String entityName)
	{
		getEntityRepository().deleteById(entityName);
	}

	@Transactional
	@Override
	public void delete(List<EntityMetaData> entities)
	{
		entities.forEach(emd -> validatePermission(emd.getName(), Permission.WRITEMETA));

		reverse(DependencyResolver.resolve(Sets.newHashSet(entities))).stream().map(EntityMetaData::getName)
				.forEach(this::deleteEntityMeta);
	}

	/**
	 * Removes an attribute from an entity.
	 */
	@Transactional
	@Override
	public void deleteAttribute(String entityName, String attributeName)
	{
		getAttributeRepository().deleteById(attributeName);
	}

	@Override
	public RepositoryCollection getBackend(EntityMetaData emd)
	{
		String backendName = emd.getBackend() == null ? getDefaultBackend().getName() : emd.getBackend();
		RepositoryCollection backend = repositoryCollectionRegistry.getRepositoryCollection(backendName);
		if (backend == null) throw new RuntimeException(format("Unknown backend [%s]", backendName));

		return backend;
	}

	@Transactional
	@Override
	public Repository<Entity> addEntityMeta(EntityMetaData entityMeta)
	{
		// create attributes
		Stream<AttributeMetaData> attrEntities = StreamSupport
				.stream(entityMeta.getOwnAttributes().spliterator(), false).flatMap(this::getAttributesPostOrder);
		getAttributeRepository().add(attrEntities);

		// create tags
		getTagRepository().add(StreamSupport.stream(entityMeta.getTags().spliterator(), false));

		// create entity
		getEntityRepository().add(entityMeta);

		return !entityMeta.isAbstract() ? getRepository(entityMeta) : null;
	}

	@Transactional
	@Override
	public void addAttribute(String fullyQualifiedEntityName, AttributeMetaData attr)
	{
		getAttributeRepository().add(attr);
	}

	@Override
	public EntityMetaData getEntityMetaData(String fullyQualifiedEntityName)
	{
		EntityMetaData systemEntity = systemEntityMetaDataRegistry.getSystemEntityMetaData(fullyQualifiedEntityName);
		if (systemEntity != null)
		{
			return systemEntity;
		}
		else
		{
			return getEntityRepository().findOneById(fullyQualifiedEntityName);
		}
	}

	@Override
	public void addPackage(Package p)
	{
		getPackageRepository().add(p);
	}

	@Override
	public Package getPackage(String string)
	{
		return getPackageRepository().findOneById(string);
	}

	@Override
	public List<Package> getPackages()
	{
		return getPackageRepository().stream().collect(toList());
	}

	@Override
	public List<Package> getRootPackages()
	{
		return dataService.query(PACKAGE, Package.class).eq(PARENT, null).findAll().collect(toList());
	}

	@Override
	public Collection<EntityMetaData> getEntityMetaDatas()
	{
		return getEntityRepository().stream().collect(toList());
	}

	@Transactional
	@Override
	public List<AttributeMetaData> updateEntityMeta(EntityMetaData entityMeta)
	{
		EntityMetaData otherEntityMeta = dataService.query(ENTITY_META_DATA, EntityMetaDataImpl.class)
				.eq(EntityMetaDataMetaData.FULL_NAME, entityMeta.getName()).findOne();
		if (otherEntityMeta == null)
		{
			throw new UnknownEntityException(format("Unknown entity [%s]", entityMeta.getName()));
		}

		// add/update attributes, attributes are deleted when deleting entity meta data if no more references exist
		Iterable<AttributeMetaData> ownAttrs = entityMeta.getOwnAttributes();
		ownAttrs.forEach(attr -> {
			if (attr.getIdentifier() == null)
			{
				dataService.add(ATTRIBUTE_META_DATA, attr);
			}
			else
			{
				AttributeMetaData existingAttr = dataService
						.findOneById(ATTRIBUTE_META_DATA, attr.getIdentifier(), AttributeMetaData.class);
				if (!EntityUtils.equals(attr, existingAttr))
				{
					dataService.update(ATTRIBUTE_META_DATA, attr);
				}
			}
		});

		// package, tag and referenced entity changes are updated separately

		// update entity
		if (!EntityUtils.equals(entityMeta, otherEntityMeta))
		{
			dataService.update(ENTITY_META_DATA, entityMeta);
		}

		return MetaUtils.updateEntityMeta(this, entityMeta);
	}

	@Override
	public Iterator<RepositoryCollection> iterator()
	{
		return repositoryCollectionRegistry.getRepositoryCollections().iterator();
	}

	@Override
	public LinkedHashMap<String, Boolean> integrationTestMetaData(RepositoryCollection repositoryCollection)
	{
		LinkedHashMap<String, Boolean> entitiesImportable = new LinkedHashMap<>();
		StreamSupport.stream(repositoryCollection.getEntityNames().spliterator(), false).forEach(
				entityName -> entitiesImportable.put(entityName, this.canIntegrateEntityMetadataCheck(
						repositoryCollection.getRepository(entityName).getEntityMetaData())));

		return entitiesImportable;
	}

	@Override
	public LinkedHashMap<String, Boolean> integrationTestMetaData(
			ImmutableMap<String, EntityMetaData> newEntitiesMetaDataMap, List<String> skipEntities,
			String defaultPackage)
	{
		LinkedHashMap<String, Boolean> entitiesImportable = new LinkedHashMap<>();

		StreamSupport.stream(newEntitiesMetaDataMap.keySet().spliterator(), false).forEach(
				entityName -> entitiesImportable.put(entityName, skipEntities.contains(entityName) || this
						.canIntegrateEntityMetadataCheck(newEntitiesMetaDataMap.get(entityName))));

		return entitiesImportable;
	}

	boolean canIntegrateEntityMetadataCheck(EntityMetaData newEntityMetaData)
	{
		String entityName = newEntityMetaData.getName();
		if (dataService.hasRepository(entityName))
		{
			EntityMetaData oldEntity = dataService.getEntityMetaData(entityName);

			List<AttributeMetaData> oldAtomicAttributes = StreamSupport
					.stream(oldEntity.getAtomicAttributes().spliterator(), false)
					.collect(Collectors.<AttributeMetaData>toList());

			LinkedHashMap<String, AttributeMetaData> newAtomicAttributesMap = new LinkedHashMap<>();
			StreamSupport.stream(newEntityMetaData.getAtomicAttributes().spliterator(), false)
					.forEach(attribute -> newAtomicAttributesMap.put(attribute.getName(), attribute));

			for (AttributeMetaData oldAttribute : oldAtomicAttributes)
			{
				if (!newAtomicAttributesMap.keySet().contains(oldAttribute.getName())) return false;

				AttributeMetaData oldAttributDefault = AttributeMetaData.newInstance(oldAttribute);
				AttributeMetaData newAttributDefault = AttributeMetaData
						.newInstance(newAtomicAttributesMap.get(oldAttribute.getName()));

				if (!oldAttributDefault.equals(newAttributDefault)) return false;
			}
		}

		return true;
	}

	@Override
	public boolean hasBackend(String backendName)
	{
		return repositoryCollectionRegistry.hasRepositoryCollection(backendName);
	}

	@Override
	public boolean isMetaEntityMetaData(EntityMetaData entityMetaData)
	{
		switch (entityMetaData.getName())
		{
			case ENTITY_META_DATA:
			case ATTRIBUTE_META_DATA:
			case TAG:
			case PACKAGE:
				return true;
			default:
				return false;
		}
	}

	private Repository<Package> getPackageRepository()
	{
		return getDefaultBackend().getRepository(packageMetaData, Package.class);
	}

	private Repository<EntityMetaData> getEntityRepository()
	{
		// FIXME hacky
		return (Repository<EntityMetaData>) (Repository<? extends EntityMetaData>) getDefaultBackend()
				.getRepository(entityMetaDataMetaData, EntityMetaDataImpl.class);
	}

	private Repository<AttributeMetaData> getAttributeRepository()
	{
		return getDefaultBackend().getRepository(attributeMetaDataMetaData, AttributeMetaData.class);
	}

	private Repository<Tag> getTagRepository()
	{
		return getDefaultBackend().getRepository(tagMetaData, Tag.class);
	}

	/**
	 * Returns child attributes of the given attribute in post-order
	 *
	 * @param attr
	 * @return
	 */
	private Stream<AttributeMetaData> getAttributesPostOrder(AttributeMetaData attr)
	{
		return StreamSupport.stream(new TreeTraverser<AttributeMetaData>()
		{
			@Override
			public Iterable<AttributeMetaData> children(AttributeMetaData attr)
			{
				return attr.getDataType() instanceof CompoundField ? attr.getAttributeParts() : emptyList();
			}
		}.postOrderTraversal(attr).spliterator(), false);
	}
}