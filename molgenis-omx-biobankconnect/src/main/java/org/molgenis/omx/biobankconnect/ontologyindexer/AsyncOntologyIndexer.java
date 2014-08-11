package org.molgenis.omx.biobankconnect.ontologyindexer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.common.collect.Iterables;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.framework.server.MolgenisSettings;
import org.molgenis.omx.biobankconnect.ontology.repository.OntologyIndexRepository;
import org.molgenis.omx.biobankconnect.ontology.repository.OntologyTermIndexRepository;
import org.molgenis.omx.biobankconnect.ontology.repository.OntologyTermQueryRepository;
import org.molgenis.omx.biobankconnect.utils.OntologyLoader;
import org.molgenis.omx.observ.target.Ontology;
import org.molgenis.omx.observ.target.OntologyTerm;
import org.molgenis.search.Hit;
import org.molgenis.search.SearchRequest;
import org.molgenis.search.SearchResult;
import org.molgenis.search.SearchService;
import org.molgenis.security.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

public class AsyncOntologyIndexer implements OntologyIndexer
{
	@Autowired
	private MolgenisSettings molgenisSettings;
	private final DataService dataService;
	private final SearchService searchService;
	private String indexingOntologyIri = null;
	private boolean isCorrectOntology = true;
	private static final String SYNONYM_FIELDS = "plugin.ontology.synonym.field";
	private static final Logger logger = Logger.getLogger(AsyncOntologyIndexer.class);

	private final AtomicInteger runningIndexProcesses = new AtomicInteger();

	@Autowired
	public AsyncOntologyIndexer(SearchService searchService, DataService dataService)
	{
		if (searchService == null) throw new IllegalArgumentException("SearchService is null!");
		if (dataService == null) throw new IllegalArgumentException("DataService is null!");
		this.searchService = searchService;
		this.dataService = dataService;
	}

	public boolean isIndexingRunning()
	{
		return (runningIndexProcesses.get() > 0);
	}

	@Override
	@Async
	@RunAsSystem
	public void index(OntologyLoader ontologyLoader)
	{
		isCorrectOntology = true;
		runningIndexProcesses.incrementAndGet();

		try
		{
			String property = molgenisSettings.getProperty(SYNONYM_FIELDS);
			if (!StringUtils.isBlank(property)) ontologyLoader.addSynonymsProperties(new HashSet<String>(Arrays
					.asList(property.split(","))));
			indexingOntologyIri = ontologyLoader.getOntologyIRI() == null ? StringUtils.EMPTY : ontologyLoader.getOntologyIRI();
			searchService.indexRepository(new OntologyIndexRepository(ontologyLoader, "ontology-" + indexingOntologyIri,
					searchService));
			searchService.indexRepository(new OntologyTermIndexRepository(ontologyLoader,
					"ontologyTerm-" + indexingOntologyIri, searchService));
		}
		catch (Exception e)
		{
			isCorrectOntology = false;
			logger.error("Exception imported file is not a valid ontology", e);
		}
		finally
		{
			String ontologyName = ontologyLoader.getOntologyName();
			if (!dataService.hasRepository(ontologyName))
			{
				Hit hit = searchService
						.search(new SearchRequest("ontology-" + indexingOntologyIri, new QueryImpl().eq(
								OntologyIndexRepository.ENTITY_TYPE, OntologyIndexRepository.TYPE_ONTOLOGY), null))
						.getSearchHits().get(0);
				Map<String, Object> columnValueMap = hit.getColumnValueMap();
				String ontologyTermEntityName = columnValueMap.containsKey(OntologyTermIndexRepository.ONTOLOGY_LABEL) ? columnValueMap
						.get(OntologyIndexRepository.ONTOLOGY_LABEL).toString() : OntologyTermQueryRepository.DEFAULT_ONTOLOGY_TERM_REPO;
				String ontologyIri = columnValueMap.containsKey(OntologyTermIndexRepository.ONTOLOGY_LABEL) ? columnValueMap
						.get(OntologyIndexRepository.ONTOLOGY_IRI).toString() : OntologyTermQueryRepository.DEFAULT_ONTOLOGY_TERM_REPO;
				dataService.addRepository(new OntologyTermQueryRepository(ontologyTermEntityName, ontologyIri,
						searchService));
			}
			runningIndexProcesses.decrementAndGet();
			indexingOntologyIri = null;
		}
	}

	@Override
	@RunAsSystem
	public void removeOntology(String ontologyIri)
	{
		Iterable<Ontology> ontologies = dataService.findAll(Ontology.ENTITY_NAME,
				new QueryImpl().eq(Ontology.IDENTIFIER, ontologyIri), Ontology.class);

		if (Iterables.size(ontologies) > 0)
		{
			for (Ontology ontology : ontologies)
			{
				Iterable<OntologyTerm> ontologyTerms = dataService.findAll(OntologyTerm.ENTITY_NAME,
						new QueryImpl().eq(OntologyTerm.ONTOLOGY, ontology), OntologyTerm.class);

				if (Iterables.size(ontologyTerms) > 0) dataService.delete(OntologyTerm.ENTITY_NAME, ontologyTerms);
			}
			dataService.delete(Ontology.ENTITY_NAME, ontologies);
		}

		SearchRequest request = new SearchRequest("ontology-" + ontologyIri, new QueryImpl().eq(
				OntologyIndexRepository.ONTOLOGY_IRI, ontologyIri), null);
		SearchResult result = searchService.search(request);
		if (result.getTotalHitCount() > 0)
		{
			Hit hit = result.getSearchHits().get(0);
			String ontologyEntityName = hit.getColumnValueMap().get(OntologyIndexRepository.ONTOLOGY_LABEL).toString();
			if (dataService.hasRepository(ontologyEntityName))
			{
				dataService.removeRepository(ontologyEntityName);
			}
		}

		searchService.deleteDocumentsByType("ontology-" + ontologyIri);
		searchService.deleteDocumentsByType("ontologyTerm-" + ontologyIri);
	}

	public String getOntologyUri()
	{
		return indexingOntologyIri;
	}

	public boolean isCorrectOntology()
	{
		return isCorrectOntology;
	}
}