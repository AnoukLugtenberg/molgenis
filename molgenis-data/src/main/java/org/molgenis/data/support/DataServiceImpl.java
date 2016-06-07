package org.molgenis.data.support;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ENTITY_META_DATA;
import static org.molgenis.data.meta.EntityMetaDataMetaData.FULL_NAME;
import static org.molgenis.security.core.utils.SecurityUtils.getCurrentUsername;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.molgenis.data.AggregateQuery;
import org.molgenis.data.AggregateResult;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityListener;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCapability;
import org.molgenis.data.SystemEntityFactory;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.meta.SystemEntity;
import org.molgenis.data.meta.system.SystemEntityMetaDataRegistry;
import org.molgenis.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the DataService interface
 */

public class DataServiceImpl implements DataService
{
	private static final Logger LOG = LoggerFactory.getLogger(DataServiceImpl.class);

	private MetaDataService metaDataService;
	private SystemEntityMetaDataRegistry systemEntityMetaDataRegistry;

	@Autowired
	public void setMetaDataService(MetaDataService metaDataService)
	{
		this.metaDataService = requireNonNull(metaDataService);
	}

	@Autowired
	public void setSystemEntityMetaDataRegistry(SystemEntityMetaDataRegistry systemEntityMetaDataRegistry)
	{
		this.systemEntityMetaDataRegistry = requireNonNull(systemEntityMetaDataRegistry);
	}

	@Override
	public EntityMetaData getEntityMetaData(String entityName)
	{
		EntityMetaData entityMetaData = systemEntityMetaDataRegistry.getSystemEntityMetaData(entityName);
		if (entityMetaData == null)
		{
			entityMetaData = query(ENTITY_META_DATA, EntityMetaData.class).eq(FULL_NAME, entityName).findOne();
		}
		return entityMetaData;
	}

	@Override
	public synchronized Stream<String> getEntityNames()
	{
		return query(ENTITY_META_DATA, EntityMetaData.class).findAll().map(EntityMetaData::getName);
	}

	// FIXME remove
	public void addRepository(Repository<Entity> repo)
	{
		throw new UnsupportedOperationException();
	}

	// FIXME remove
	public void removeRepository(String entityName)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasRepository(String entityName)
	{
		return metaDataService.hasRepository(entityName);
	}

	@Override
	public long count(String entityName)
	{
		return getRepository(entityName).count();
	}

	@Override
	public long count(String entityName, Query<Entity> q)
	{
		return getRepository(entityName).count(q);
	}

	@Override
	public Stream<Entity> findAll(String entityName)
	{
		return findAll(entityName, query(entityName));
	}

	@Override
	public Stream<Entity> findAll(String entityName, Query<Entity> q)
	{
		return getRepository(entityName).findAll(q);
	}

	@Override
	public Entity findOneById(String entityName, Object id)
	{
		return getRepository(entityName).findOneById(id);
	}

	@Override
	public Entity findOne(String entityName, Query<Entity> q)
	{
		return getRepository(entityName).findOne(q);
	}

	@Override
	@Transactional
	public void add(String entityName, Entity entity)
	{
		getRepository(entityName).add(entity);
	}

	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public <E extends Entity> void add(String entityName, Stream<E> entities)
	{
		getRepository(entityName).add((Stream<Entity>) entities);
	}

	@Override
	@Transactional
	public void update(String entityName, Entity entity)
	{
		getRepository(entityName).update(entity);
	}

	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public <E extends Entity> void update(String entityName, Stream<E> entities)
	{
		getRepository(entityName).update((Stream<Entity>) entities);
	}

	@Override
	@Transactional
	public void delete(String entityName, Entity entity)
	{
		getRepository(entityName).delete(entity);
	}

	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public <E extends Entity> void delete(String entityName, Stream<E> entities)
	{
		getRepository(entityName).delete((Stream<Entity>) entities);
	}

	@Override
	@Transactional
	public void deleteById(String entityName, Object id)
	{
		getRepository(entityName).deleteById(id);
	}

	@Override
	@Transactional
	public void deleteAll(String entityName)
	{
		getRepository(entityName).deleteAll();
		LOG.info("All entities of repository [{}] deleted by user [{}]", entityName, getCurrentUsername());
	}

	@Override
	public Repository<Entity> getRepository(String entityName)
	{
		return metaDataService.getRepository(entityName);
	}

	@SuppressWarnings("unchecked")
	public <E extends SystemEntity> Repository<E> getRepository(String entityName, Class<E> entityClass)
	{
		Repository<Entity> untypedRepo = getRepository(entityName);
		SystemEntityFactory<E, Object> systemEntityFactory = systemEntityMetaDataRegistry
				.getSystemEntityFactory(entityClass);
		return new TypedRepositoryDecorator<>(untypedRepo, systemEntityFactory);
	}

	@Override
	public Query<Entity> query(String entityName)
	{
		return new QueryImpl<>(getRepository(entityName));
	}

	@Override
	public <E extends SystemEntity> Query<E> query(String entityName, Class<E> entityClass)
	{
		return new QueryImpl<>(getRepository(entityName, entityClass));
	}

	@Override
	public <E extends SystemEntity> Stream<E> findAll(String entityName, Query<E> q, Class<E> clazz)
	{
		return getRepository(entityName, clazz).findAll(q);
	}

	@Override
	public <E extends SystemEntity> E findOneById(String entityName, Object id, Class<E> clazz)
	{
		return getRepository(entityName, clazz).findOneById(id);
	}

	@Override
	public <E extends SystemEntity> E findOne(String entityName, Query<E> q, Class<E> clazz)
	{
		Entity entity = getRepository(entityName, clazz).findOne(q);
		if (entity == null) return null;
		return EntityUtils.convert(entity, clazz, this);
	}

	@Override
	public <E extends SystemEntity> Stream<E> findAll(String entityName, Class<E> clazz)
	{
		return findAll(entityName, query(entityName, clazz), clazz);
	}

	@Override
	public AggregateResult aggregate(String entityName, AggregateQuery aggregateQuery)
	{
		return getRepository(entityName).aggregate(aggregateQuery);
	}

	@Override
	public MetaDataService getMeta()
	{
		return metaDataService;
	}

	@Override
	public synchronized Iterator<Repository<Entity>> iterator()
	{
		throw new UnsupportedOperationException(); // FIXME
		//		return Lists.newArrayList(repositories.values()).iterator();
	}

	@Override
	public Stream<Entity> stream(String entityName, Fetch fetch)
	{
		return getRepository(entityName).stream(fetch);
	}

	@Override
	public <E extends SystemEntity> Stream<E> stream(String entityName, Fetch fetch, Class<E> clazz)
	{
		Stream<Entity> entities = getRepository(entityName).stream(fetch);
		return entities.map(entity -> {
			return EntityUtils.convert(entity, clazz, this);
		});
	}

	@Override
	public Set<RepositoryCapability> getCapabilities(String repositoryName)
	{
		return getRepository(repositoryName).getCapabilities();
	}

	@Override
	public Entity findOneById(String entityName, Object id, Fetch fetch)
	{
		return getRepository(entityName).findOneById(id, fetch);
	}

	@Override
	public <E extends SystemEntity> E findOneById(String entityName, Object id, Fetch fetch, Class<E> clazz)
	{
		Entity entity = getRepository(entityName).findOneById(id, fetch);
		if (entity == null) return null;
		return EntityUtils.convert(entity, clazz, this);
	}

	@Override
	public void addEntityListener(String entityName, EntityListener entityListener)
	{
		getRepository(entityName).addEntityListener(entityListener);
	}

	@Override
	public void removeEntityListener(String entityName, EntityListener entityListener)
	{
		getRepository(entityName).removeEntityListener(entityListener);
	}

	@Override
	public Stream<Entity> findAll(String entityName, Stream<Object> ids)
	{
		return getRepository(entityName).findAll(ids);
	}

	@Override
	public <E extends SystemEntity> Stream<E> findAll(String entityName, Stream<Object> ids, Class<E> clazz)
	{
		Stream<Entity> entities = getRepository(entityName).findAll(ids);
		return entities.map(entity -> {
			return EntityUtils.convert(entity, clazz, this);
		});
	}

	@Override
	public Stream<Entity> findAll(String entityName, Stream<Object> ids, Fetch fetch)
	{
		return getRepository(entityName).findAll(ids, fetch);
	}

	@Override
	public <E extends SystemEntity> Stream<E> findAll(String entityName, Stream<Object> ids, Fetch fetch,
			Class<E> clazz)
	{
		Stream<Entity> entities = getRepository(entityName).findAll(ids, fetch);
		return entities.map(entity -> EntityUtils.convert(entity, clazz, this));
	}

	@Override
	public Repository<Entity> copyRepository(Repository<Entity> repository, String newRepositoryId,
			String newRepositoryLabel)
	{
		return copyRepository(repository, newRepositoryId, newRepositoryLabel, new QueryImpl<Entity>());
	}

	@Override
	public Repository<Entity> copyRepository(Repository<Entity> repository, String newRepositoryId,
			String newRepositoryLabel, Query<Entity> query)
	{
		LOG.info("Creating a copy of " + repository.getName() + " repository, with ID: " + newRepositoryId
				+ ", and label: " + newRepositoryLabel);
		EntityMetaData emd = EntityMetaData.newInstance(repository.getEntityMetaData());
		emd.setName(newRepositoryId);
		emd.setLabel(newRepositoryLabel);
		Repository<Entity> repositoryCopy = metaDataService.addEntityMeta(emd);
		try
		{

			repositoryCopy.add(repository.findAll(query));
			return repositoryCopy;
		}
		catch (RuntimeException e)
		{
			if (repositoryCopy != null)
			{
				metaDataService.deleteEntityMeta(emd.getName());
			}
			throw e;
		}
	}
}
