package org.molgenis.data.idcard;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.RepositoryCapability.AGGREGATEABLE;
import static org.molgenis.data.RepositoryCapability.MANAGABLE;
import static org.molgenis.data.RepositoryCapability.QUERYABLE;
import static org.molgenis.data.RepositoryCapability.WRITABLE;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.molgenis.data.AggregateQuery;
import org.molgenis.data.AggregateResult;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityListener;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCapability;
import org.molgenis.data.elasticsearch.ElasticsearchService;
import org.molgenis.data.elasticsearch.ElasticsearchService.IndexingMode;
import org.molgenis.data.idcard.client.IdCardClient;
import org.molgenis.data.idcard.model.IdCardBiobank;
import org.molgenis.data.idcard.model.IdCardBiobankMetaData;
import org.molgenis.data.support.QueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

@org.springframework.stereotype.Repository
public class IdCardBiobankRepository implements Repository // TODO check if we can extend from AbstractRepository
{
	private static final Logger LOG = LoggerFactory.getLogger(IdCardBiobankRepository.class);

	private static final long ID_CARD_INDEX_REBUILD_TIMEOUT = 60000l; // 60s // FIXME add to settings

	private final IdCardBiobankMetaData idCardBiobankMetaData;
	private final IdCardClient idCardClient;
	private final ElasticsearchService elasticsearchService;
	private final DataService dataService;

	@Autowired
	public IdCardBiobankRepository(IdCardBiobankMetaData idCardBiobankMetaData, IdCardClient idCardClient,
			ElasticsearchService elasticsearchService, DataService dataService)
	{
		this.idCardBiobankMetaData = idCardBiobankMetaData;
		this.idCardClient = requireNonNull(idCardClient);
		this.elasticsearchService = requireNonNull(elasticsearchService);
		this.dataService = requireNonNull(dataService);
	}

	@Override
	public Iterator<Entity> iterator()
	{
		return idCardClient.getIdCardBiobanks().iterator();
	}

	@Override
	public void close() throws IOException
	{
		// noop
	}

	@Override
	public Set<RepositoryCapability> getCapabilities()
	{
		Set<RepositoryCapability> repoCapabilities = new HashSet<>();
		repoCapabilities.add(AGGREGATEABLE);
		repoCapabilities.add(QUERYABLE);
		return repoCapabilities;
	}

	@Override
	public String getName()
	{
		return IdCardBiobank.ENTITY_NAME;
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		return idCardBiobankMetaData;
	}

	@Override
	public long count()
	{
		return elasticsearchService.count(getEntityMetaData());
	}

	@Override
	public Query query()
	{
		return new QueryImpl();
	}

	@Override
	public long count(Query q)
	{
		return elasticsearchService.count(q, getEntityMetaData());
	}

	@Override
	public Iterable<Entity> findAll(Query q)
	{
		return elasticsearchService.search(q, getEntityMetaData());
	}

	@Override
	public Entity findOne(Query q)
	{
		Iterator<Entity> it = findAll(q).iterator();
		return it.hasNext() ? it.next() : null;
	}

	@Override
	public Entity findOne(Object id)
	{
		try
		{
			return idCardClient.getIdCardBiobank(id.toString());
		}
		catch (RuntimeException e)
		{
			return createErrorIdCardBiobank(id);
		} // FIXME get rid of cast
	}

	@Override
	public Entity findOne(Object id, Fetch fetch)
	{
		return findOne(id);
	}

	@Override
	public Iterable<Entity> findAll(Iterable<Object> ids)
	{
		try
		{
			return idCardClient.getIdCardBiobanks(Iterables.transform(ids, new Function<Object, String>()
			{
				@Override
				public String apply(Object id)
				{
					return id.toString();
				}
			}));
		}
		catch (RuntimeException e)
		{
			return new Iterable<Entity>()
			{
				@Override
				public Iterator<Entity> iterator()
				{
					return StreamSupport.stream(ids.spliterator(), false).map(id -> {
						return (Entity) createErrorIdCardBiobank(id);
					}).iterator(); // FIXME get rid of cast
				}
			};
		}
	}

	@Override
	public Iterable<Entity> findAll(Iterable<Object> ids, Fetch fetch)
	{
		return findAll(ids);
	}

	@Override
	public AggregateResult aggregate(AggregateQuery aggregateQuery)
	{
		return elasticsearchService.aggregate(aggregateQuery, getEntityMetaData());
	}

	@Override
	public void update(Entity entity)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void update(Iterable<? extends Entity> records)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void delete(Entity entity)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void delete(Iterable<? extends Entity> entities)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void deleteById(Object id)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void deleteById(Iterable<Object> ids)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void deleteAll()
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void add(Entity entity)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public Integer add(Iterable<? extends Entity> entities)
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), WRITABLE.toString()));
	}

	@Override
	public void flush()
	{
		elasticsearchService.flush();
	}

	@Override
	public void clearCache()
	{
		// noop
	}

	@Override
	public void addEntityListener(EntityListener entityListener)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEntityListener(EntityListener entityListener)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void rebuildIndex()
	{
		LOG.trace("Indexing ID-Card biobanks ...");
		Iterable<? extends Entity> entities = idCardClient.getIdCardBiobanks(ID_CARD_INDEX_REBUILD_TIMEOUT);

		EntityMetaData entityMeta = getEntityMetaData();
		if (!elasticsearchService.hasMapping(entityMeta))
		{
			elasticsearchService.createMappings(entityMeta);
		}
		elasticsearchService.index(entities, entityMeta, IndexingMode.UPDATE);
		LOG.debug("Indexed ID-Card biobanks");
	}

	private IdCardBiobank createErrorIdCardBiobank(Object id)
	{
		IdCardBiobank idCardBiobank = new IdCardBiobank(dataService);
		idCardBiobank.set(IdCardBiobank.ORGANIZATION_ID, id);
		idCardBiobank.set(IdCardBiobank.NAME, "Error loading data");
		return idCardBiobank; // FIXME get rid of cast
	}

	@Override
	public void create()
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), MANAGABLE.toString()));
	}

	@Override
	public void drop()
	{
		throw new UnsupportedOperationException(
				String.format("Repository [%s] is not %s", getName(), MANAGABLE.toString()));
	}
}
