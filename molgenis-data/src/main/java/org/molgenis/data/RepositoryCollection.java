package org.molgenis.data;

/**
 * Repository collection
 */
public interface RepositoryCollection
{
	/**
	 * Get names of all the entities in this source
	 */
	Iterable<String> getEntityNames();

	// TODO remove
	/**
	 * Get a repository by entity name
	 * 
	 * @throws UnknownEntityException
	 */
	Repository getRepositoryByEntityName(String name);
}
