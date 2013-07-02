package org.molgenis.omx.ontologyIndexer.plugin;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.framework.db.Database;
import org.molgenis.omx.ontologyIndexer.table.OntologyModel;
import org.molgenis.omx.ontologyIndexer.table.OntologyTable;
import org.molgenis.omx.ontologyIndexer.table.OntologyTermTable;
import org.molgenis.search.SearchService;
import org.molgenis.util.DatabaseUtil;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

public class AsyncOntologyIndexer implements OntologyIndexer, InitializingBean
{
	private SearchService searchService;
	private String ontologyUri = null;
	private boolean isCorrectOntology = true;

	private final AtomicInteger runningIndexProcesses = new AtomicInteger();

	@Autowired
	public void setSearchService(SearchService searchService)
	{
		this.searchService = searchService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if (searchService == null) throw new IllegalArgumentException("Missing bean of type SearchService");
	}

	@Override
	public boolean isIndexingRunning()
	{
		return (runningIndexProcesses.get() > 0);
	}

	@Async
	public void index(File ontologyFile)
	{
		isCorrectOntology = true;
		runningIndexProcesses.incrementAndGet();
		Database db = DatabaseUtil.createDatabase();

		try
		{
			OntologyModel model = new OntologyModel(ontologyFile);
			ontologyUri = model.getOntologyIRI() == null ? StringUtils.EMPTY : model.getOntologyIRI();
			searchService.indexTupleTable("ontology" + ontologyUri, new OntologyTable(model, db));
			searchService.indexTupleTable("ontologyTerm" + ontologyUri, new OntologyTermTable(model, db));
		}
		catch (OWLOntologyCreationException e)
		{
			isCorrectOntology = false;
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtil.closeQuietly(db);
			runningIndexProcesses.decrementAndGet();
			ontologyUri = null;
		}
	}

	public String getOntologyUri()
	{
		return ontologyUri;
	}

	@Override
	public boolean isCorrectOntology()
	{
		return this.isCorrectOntology;
	}
}
