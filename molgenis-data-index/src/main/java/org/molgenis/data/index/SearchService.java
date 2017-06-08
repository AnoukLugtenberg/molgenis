package org.molgenis.data.index;

import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.aggregation.AggregateQuery;
import org.molgenis.data.aggregation.AggregateResult;
import org.molgenis.data.meta.model.EntityType;

import java.util.stream.Stream;

public interface SearchService
{
	void createIndex(EntityType entityType);

	boolean hasIndex(EntityType entityType);

	void deleteIndex(EntityType entityType);

	long count(EntityType entityType);

	long count(EntityType entityType, Query<Entity> q);

	void index(EntityType entityType, Entity entity, IndexingMode indexingMode);

	/**
	 * Adds or updated the given entities in the index
	 *
	 * @param entityType
	 * @param entities
	 * @param indexingMode
	 * @return number of indexed entities, which equals the size of the input entities iterable
	 */
	long index(EntityType entityType, Iterable<? extends Entity> entities, IndexingMode indexingMode);

	/**
	 * Adds or updated the given entities in the index
	 *
	 * @param entityType
	 * @param entities
	 * @param indexingMode
	 * @return number of indexed entities
	 */
	long index(EntityType entityType, Stream<? extends Entity> entities, IndexingMode indexingMode);

	void delete(EntityType entityType, Entity entity);

	void deleteById(EntityType entityType, String id);

	void deleteById(EntityType entityType, Stream<String> ids);

	void delete(EntityType entityType, Iterable<? extends Entity> entities);

	/**
	 * Deletes entities from index
	 *
	 * @param entityType
	 * @param entities       entity stream
	 */
	void delete(EntityType entityType, Stream<? extends Entity> entities);

	// TODO replace Iterable<Entity> with EntityCollection and add EntityCollection.getTotal()
	Iterable<Entity> search(EntityType entityType, Query<Entity> q);

	/**
	 * TODO replace Stream<Entity> with EntityCollection and add EntityCollection.getTotal()
	 *
	 * @param entityType
	 * @param q
	 * @return
	 */
	Stream<Entity> searchAsStream(EntityType entityType, Query<Entity> q);

	AggregateResult aggregate(EntityType entityType, AggregateQuery aggregateQuery);

	void rebuildIndex(Repository<? extends Entity> repository);

	void refreshIndex();

	Entity findOne(EntityType entityType, Query<Entity> q);
}