package org.molgenis.data.importer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.molgenis.data.CrudRepository;
import org.molgenis.data.DataService;
import org.molgenis.data.DatabaseAction;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IndexedRepository;
import org.molgenis.data.ManageableCrudRepositoryCollection;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Package;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.meta.TagMetaData;
import org.molgenis.data.meta.WritableMetaDataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.data.support.TransformedEntity;
import org.molgenis.fieldtypes.FieldType;
import org.molgenis.framework.db.EntityImportReport;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.permission.PermissionSystemService;
import org.molgenis.util.DependencyResolver;
import org.molgenis.util.HugeSet;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Writes the imported metadata and data to target {@link RepositoryCollection}.
 */
public class ImportWriter
{
	private final PermissionSystemService permissionSystemService;

	private final DataService dataService;
	private final WritableMetaDataService metaDataService;

	private final static Logger logger = Logger.getLogger(ImportWriter.class);

	public ImportWriter(DataService dataService, WritableMetaDataService metaDataService,
			PermissionSystemService permissionSystemService)
	{
		this.permissionSystemService = permissionSystemService;
		this.dataService = dataService;
		this.metaDataService = metaDataService;
	}

	@Transactional
	public EntityImportReport doTransactionalImport(EmxImportJob job)
	{
		importTags(job.source);
		importPackages(job.parsedMetaData);
		addEntityMetaData(job.parsedMetaData, job.report, job.metaDataChanges, job.target);
		addEntityPermissions(job.metaDataChanges);
		importData(job.report, job.parsedMetaData.getEntities(), job.source, job.target, job.dbAction);
		return job.report;
	}

	private void importData(EntityImportReport report, List<EntityMetaData> resolved, RepositoryCollection source,
			RepositoryCollection targetCollection, DatabaseAction dbAction)
	{
		for (final EntityMetaData entityMetaData : resolved)
		{
			String name = entityMetaData.getName();
			CrudRepository crudRepository = (CrudRepository) targetCollection.getRepositoryByEntityName(name);

			if (crudRepository != null)
			{
				Repository fileEntityRepository = source.getRepositoryByEntityName(entityMetaData.getSimpleName());

				// check to prevent nullpointer when importing metadata only
				if (fileEntityRepository != null)
				{
					// transforms entities so that they match the entity meta data of the output repository
					Iterable<Entity> entities = Iterables.transform(fileEntityRepository,
							new Function<Entity, Entity>()
							{
								@Override
								public Entity apply(Entity entity)
								{
									return new TransformedEntity(entity, entityMetaData, dataService);
								}
							});
					entities = DependencyResolver.resolveSelfReferences(entities, entityMetaData);

					int count = update(crudRepository, entities, dbAction);
					report.addEntityCount(name, count);
				}
			}
		}
	}

	/**
	 * Gives the user permission to see and edit his imported entities, unless the user is admin since admins can do
	 * that anyways.
	 */
	private void addEntityPermissions(MetaDataChanges metaDataChanges)
	{
		if (!SecurityUtils.currentUserIsSu())
		{
			permissionSystemService.giveUserEntityAndMenuPermissions(SecurityContextHolder.getContext(),
					metaDataChanges.getAddedEntities());
		}
	}

	private void addEntityMetaData(ParsedMetaData parsedMetaData, EntityImportReport report,
			MetaDataChanges metaDataChanges, ManageableCrudRepositoryCollection targetCollection)
	{
		for (EntityMetaData entityMetaData : parsedMetaData.getEntities())
		{
			String name = entityMetaData.getName();
			if (!EmxMetaDataParser.ENTITIES.equals(name) && !EmxMetaDataParser.ATTRIBUTES.equals(name)
					&& !EmxMetaDataParser.PACKAGES.equals(name) && !EmxMetaDataParser.TAGS.equals(name))
			{
				if (metaDataService.getEntityMetaData(entityMetaData.getName()) == null)
				{
					logger.debug("tyring to create: " + name);
					metaDataChanges.addEntity(name);
					Repository repo = targetCollection.add(entityMetaData);
					if (repo != null)
					{
						report.addNewEntity(name);
					}
				}
				else if (!entityMetaData.isAbstract())
				{
					metaDataChanges.addAttributes(name, targetCollection.update(entityMetaData));
				}
			}
		}
	}

	private void importPackages(ParsedMetaData parsedMetaData)
	{
		for (Package p : parsedMetaData.getPackages().values())
		{
			if (p != null)
			{
				metaDataService.addPackage(p);
			}
		}
	}

	private void importTags(RepositoryCollection source)
	{
		Repository tagRepo = source.getRepositoryByEntityName(TagMetaData.ENTITY_NAME);
		if (tagRepo != null)
		{
			for (Entity tag : tagRepo)
			{
				Entity transformed = new TransformedEntity(tag, new TagMetaData(), dataService);
				Entity existingTag = dataService
						.findOne(TagMetaData.ENTITY_NAME, tag.getString(TagMetaData.IDENTIFIER));

				if (existingTag == null)
				{
					dataService.add(TagMetaData.ENTITY_NAME, transformed);
				}
				else
				{
					dataService.update(TagMetaData.ENTITY_NAME, transformed);
				}
			}
		}
	}

	private void rollbackSchemaChanges(RepositoryCollection source,
			ManageableCrudRepositoryCollection targetCollection, List<String> addedEntities,
			Map<String, List<String>> addedAttributes)
	{
		logger.info("Rolling back changes.");
		dropAddedEntities(targetCollection, addedEntities);
		List<String> entities = dropAddedAttributes(targetCollection, addedAttributes);

		// Reindex
		Set<String> entitiesToIndex = Sets.newLinkedHashSet(source.getEntityNames());
		entitiesToIndex.addAll(entities);
		entitiesToIndex.add("tags");
		entitiesToIndex.add("packages");
		entitiesToIndex.add("entities");
		entitiesToIndex.add("attributes");

		reindex(entitiesToIndex);
	}

	private void reindex(Set<String> entitiesToIndex)
	{
		for (String entity : entitiesToIndex)
		{
			if (dataService.hasRepository(entity))
			{
				Repository repo = dataService.getRepositoryByEntityName(entity);
				if ((repo != null) && (repo instanceof IndexedRepository))
				{
					((IndexedRepository) repo).rebuildIndex();
				}
			}
		}
	}

	private List<String> dropAddedAttributes(ManageableCrudRepositoryCollection targetCollection,
			Map<String, List<String>> addedAttributes)
	{
		List<String> entities = Lists.newArrayList(addedAttributes.keySet());
		Collections.reverse(entities);

		for (String entityName : entities)
		{
			List<String> attributes = addedAttributes.get(entityName);
			for (String attributeName : attributes)
			{
				targetCollection.dropAttributeMetaData(entityName, attributeName);
			}
		}
		return entities;
	}

	private void dropAddedEntities(ManageableCrudRepositoryCollection targetCollection, List<String> addedEntities)
	{
		// Rollback metadata, create table statements cannot be rolled back, we have to do it ourselves
		Collections.reverse(addedEntities);

		for (String entityName : addedEntities)
		{
			targetCollection.dropEntityMetaData(entityName);
		}
	}

	public int update(CrudRepository repo, Iterable<? extends Entity> entities, DatabaseAction dbAction)
	{
		if (entities == null) return 0;

		String idAttributeName = repo.getEntityMetaData().getIdAttribute().getName();
		FieldType idDataType = repo.getEntityMetaData().getIdAttribute().getDataType();

		HugeSet<Object> existingIds = new HugeSet<Object>();
		HugeSet<Object> ids = new HugeSet<Object>();
		try
		{
			for (Entity entity : entities)
			{
				Object id = entity.get(idAttributeName);
				if (id != null)
				{
					ids.add(id);
				}
			}

			if (!ids.isEmpty())
			{
				// Check if the ids already exist
				if (repo.count() > 0)
				{
					int batchSize = 100;
					Query q = new QueryImpl();
					Iterator<Object> it = ids.iterator();
					int batchCount = 0;
					while (it.hasNext())
					{
						q.eq(idAttributeName, it.next());
						batchCount++;
						if (batchCount == batchSize || !it.hasNext())
						{
							for (Entity existing : repo.findAll(q))
							{
								existingIds.add(existing.getIdValue());
							}
							q = new QueryImpl();
							batchCount = 0;
						}
						else
						{
							q.or();
						}
					}
				}
			}

			int count = 0;
			switch (dbAction)
			{
				case ADD:
					if (!existingIds.isEmpty())
					{
						StringBuilder msg = new StringBuilder();
						msg.append("Trying to add existing ").append(repo.getName())
								.append(" entities as new insert: ");

						int i = 0;
						Iterator<?> it = existingIds.iterator();
						while (it.hasNext() && i < 5)
						{
							if (i > 0)
							{
								msg.append(",");
							}
							msg.append(it.next());
							i++;
						}

						if (it.hasNext())
						{
							msg.append(" and more.");
						}
						throw new MolgenisDataException(msg.toString());
					}
					count = repo.add(entities);
					break;

				case ADD_UPDATE_EXISTING:
					int batchSize = 1000;
					List<Entity> existingEntities = Lists.newArrayList();
					List<Entity> newEntities = Lists.newArrayList();

					Iterator<? extends Entity> it = entities.iterator();
					while (it.hasNext())
					{
						Entity entity = it.next();
						count++;
						Object id = idDataType.convert(entity.get(idAttributeName));
						if (existingIds.contains(id))
						{
							existingEntities.add(entity);
							if (existingEntities.size() == batchSize)
							{
								repo.update(existingEntities);
								existingEntities.clear();
							}
						}
						else
						{
							newEntities.add(entity);
							if (newEntities.size() == batchSize)
							{
								repo.add(newEntities);
								newEntities.clear();
							}
						}
					}

					if (!existingEntities.isEmpty())
					{
						repo.update(existingEntities);
					}

					if (!newEntities.isEmpty())
					{
						repo.add(newEntities);
					}
					break;

				case UPDATE:
					int errorCount = 0;
					StringBuilder msg = new StringBuilder();
					msg.append("Trying to update not exsisting ").append(repo.getName()).append(" entities:");

					for (Entity entity : entities)
					{
						count++;
						Object id = idDataType.convert(entity.get(idAttributeName));
						if (!existingIds.contains(id))
						{
							if (++errorCount == 6)
							{
								break;
							}

							if (errorCount > 0)
							{
								msg.append(", ");
							}
							msg.append(id);
						}
					}

					if (errorCount > 0)
					{
						if (errorCount == 6)
						{
							msg.append(" and more.");
						}
						throw new MolgenisDataException(msg.toString());
					}
					repo.update(entities);
					break;

				default:
					break;

			}

			return count;
		}
		finally
		{
			IOUtils.closeQuietly(existingIds);
			IOUtils.closeQuietly(ids);
		}
	}

	public EntityImportReport doImport(EmxImportJob job)
	{
		try
		{
			return doTransactionalImport(job);
		}
		catch (Exception e)
		{
			rollbackSchemaChanges(job.source, job.target, null, null);
			throw e;
		}
		finally
		{
			metaDataService.refreshCaches();
		}
	}
}