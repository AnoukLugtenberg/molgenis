package org.molgenis.omx.studymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.molgenis.catalog.CatalogItem;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.observ.target.Ontology;
import org.molgenis.omx.observ.target.OntologyTerm;

import com.google.common.collect.Lists;

public class OmxStudyDefinitionItem implements CatalogItem
{
	private final ObservableFeature observableFeature;
	private final Integer catalogId;

	public OmxStudyDefinitionItem(ObservableFeature observableFeature, Integer catalogId)
	{
		if (observableFeature == null) throw new IllegalArgumentException("observableFeature is null");
		if (catalogId == null) throw new IllegalArgumentException("catalogId is null");
		this.observableFeature = observableFeature;
		this.catalogId = catalogId;
	}

	@Override
	public String getId()
	{
		return observableFeature.getId().toString();
	}

	@Override
	public String getName()
	{
		return observableFeature.getName();
	}

	@Override
	public String getDescription()
	{
		return observableFeature.getDescription();
	}

	@Override
	public String getCode()
	{
		List<OntologyTerm> ontologyTerm = observableFeature.getDefinitions();
		if (ontologyTerm == null || ontologyTerm.isEmpty()) return null;
		else if (ontologyTerm.size() > 1) throw new RuntimeException("Multiple ontology terms are not supported");
		else return ontologyTerm.get(0).getTermAccession();
	}

	@Override
	public String getCodeSystem()
	{
		List<OntologyTerm> ontologyTerm = observableFeature.getDefinitions();
		if (ontologyTerm == null || ontologyTerm.isEmpty()) return null;
		else if (ontologyTerm.size() > 1) throw new RuntimeException("Multiple ontology terms are not supported");
		else
		{
			Ontology ontology = ontologyTerm.get(0).getOntology();
			return ontology != null ? ontology.getOntologyAccession() : null;
		}
	}

	@Override
	public List<String> getPath()
	{
		List<String> protocolPath = new ArrayList<String>();
		Collection<Protocol> protocols = observableFeature.getFeaturesProtocolCollection();
		boolean rootReached = false;
		while (protocols != null && !protocols.isEmpty() && !rootReached)
		{
			if (protocols.size() != 1)
			{
				throw new RuntimeException("Catalog item (group) must belong to one catalog (instead of "
						+ protocols.size() + ')');
			}
			Protocol protocol = protocols.iterator().next();
			// Stop when catalog protocol is found (this is the root)
			if (protocol.getId().equals(catalogId)) rootReached = true;
			protocolPath.add(protocol.getId().toString());
			protocols = protocol.getSubprotocolsProtocolCollection();
		}

		return Lists.reverse(protocolPath);
	}
}
