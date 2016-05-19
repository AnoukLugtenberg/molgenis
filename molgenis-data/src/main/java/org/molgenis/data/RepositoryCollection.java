package org.molgenis.data;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.meta.EntityMetaDataImpl;

/**
 * Repository collection
 */
public interface RepositoryCollection extends Iterable<Repository<Entity>>
{
	/**
	 * @return the name of this backend
	 */
	String getName();

	/**
	 * Streams the {@link Repository}s
	 */
	default Stream<Repository<Entity>> stream()
	{
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Create and add a new CrudRepository for an EntityMetaData
	 */
	Repository<Entity> createRepository(EntityMetaData entityMeta);

	/**
	 * Get names of all the entities in this source
	 */
	Iterable<String> getEntityNames();

	/**
	 * Get a repository by entity name
	 * 
	 * @throws UnknownEntityException
	 */
	Repository<Entity> getRepository(String name);

	/**
	 * Get a repository for the given entity meta data
	 *
	 * @param entityMeta
	 * @return
	 */
	Repository<Entity> getRepository(EntityMetaData entityMeta);

	/**
	 * Check if a repository exists by entity name
	 *
	 */
	boolean hasRepository(String name);

	boolean hasRepository(EntityMetaData entityMeta);

	/**
	 * Removes an entity definition from this ManageableCrudRepositoryCollection
	 *
	 * @param entityName
	 */
	void deleteRepository(String entityName);

	/**
	 * Adds an Attribute to an EntityMeta
	 *
	 * @param entityName
	 * @param attribute
	 */
	void addAttribute(String entityName, AttributeMetaData attribute);

	/**
	 * Removes an attribute from an entity
	 *
	 * @param entityName
	 * @param attributeName
	 */
	void deleteAttribute(String entityName, String attributeName);

}
