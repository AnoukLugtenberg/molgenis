package org.molgenis.ontology.repository;

import static org.molgenis.ontology.model.OntologyMetaData.ENTITY_NAME;
import static org.molgenis.ontology.model.OntologyMetaData.ID;
import static org.molgenis.ontology.model.OntologyMetaData.ONTOLOGY_IRI;
import static org.molgenis.ontology.model.OntologyMetaData.ONTOLOGY_NAME;

import org.elasticsearch.common.collect.Iterables;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.repository.model.Ontology;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Maps OntologyMetaData {@link Entity} <-> {@link Ontology}
 * 
 */
public class OntologyRepository
{
	@Autowired
	private DataService dataService;

	/**
	 * Retrieves all {@link Ontology}s.
	 */
	public Iterable<Ontology> getOntologies()
	{
		return Iterables.transform(dataService.findAll(ENTITY_NAME), OntologyRepository::toOntology);
	}

	/**
	 * Retrieves an ontology with a specific name.
	 * 
	 * @param name
	 *            the name of the repository
	 * @return
	 */
	public Ontology getOntology(String name)
	{
		return toOntology(dataService.findOne(ENTITY_NAME, QueryImpl.EQ(ONTOLOGY_NAME, name)));
	}

	private static Ontology toOntology(Entity entity)
	{
		if (entity == null)
		{
			return null;
		}
		return Ontology.create(entity.getString(ID), entity.getString(ONTOLOGY_IRI), entity.getString(ONTOLOGY_NAME));
	}

}
