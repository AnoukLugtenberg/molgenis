package org.molgenis.data.semanticsearch.service.impl;

import static org.elasticsearch.common.collect.Lists.newArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.collect.Lists;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.meta.MetaUtils;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

public class SemanticSearchServiceImpl implements SemanticSearchService
{
	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private MetaDataService metaDataService;

	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyTagService ontologyTagService;

	@Autowired
	private SemanticSearchServiceHelper semanticSearchServiceHelper;

	@Override
	public Map<String, List<AttributeMetaData>> findAttributes(EntityMetaData sourceEntityMetaData,
			EntityMetaData targetEntityMetaData)
	{
		Iterable<AttributeMetaData> attributes = targetEntityMetaData.getAtomicAttributes();
		Map<String, List<AttributeMetaData>> suggestedEntityMappings = new HashMap<String, List<AttributeMetaData>>();

		attributes.forEach(attribute -> suggestedEntityMappings.put(attribute.getName(),
				newArrayList(findAttributes(sourceEntityMetaData, targetEntityMetaData, attribute))));

		return suggestedEntityMappings;
	}

	@Override
	public Iterable<AttributeMetaData> findAttributes(EntityMetaData sourceEntityMetaData,
			EntityMetaData targetEntityMetaData, AttributeMetaData targetAttribute)
	{
		Iterable<String> attributeIdentifiers = semanticSearchServiceHelper
				.getAttributeIdentifiers(sourceEntityMetaData);

		QueryRule createDisMaxQueryRule = semanticSearchServiceHelper.createDisMaxQueryRule(targetEntityMetaData,
				targetAttribute);

		List<QueryRule> disMaxQueryRules = Lists.newArrayList(new QueryRule(AttributeMetaDataMetaData.IDENTIFIER,
				Operator.IN, attributeIdentifiers));

		if (createDisMaxQueryRule.getNestedRules().size() > 0)
		{
			disMaxQueryRules.addAll(Arrays.asList(new QueryRule(Operator.AND), createDisMaxQueryRule));
		}

		Iterable<Entity> attributeMetaDataEntities = dataService.findAll(AttributeMetaDataMetaData.ENTITY_NAME,
				new QueryImpl(disMaxQueryRules));

		return Iterables.size(attributeMetaDataEntities) > 0 ? MetaUtils.toExistingAttributeMetaData(
				sourceEntityMetaData, attributeMetaDataEntities) : sourceEntityMetaData.getAttributes();
	}

	@Override
	public Map<AttributeMetaData, List<OntologyTerm>> findTags(String entity, List<String> ontologyIds)
	{
		Map<AttributeMetaData, List<OntologyTerm>> result = new LinkedHashMap<AttributeMetaData, List<OntologyTerm>>();
		EntityMetaData emd = metaDataService.getEntityMetaData(entity);
		for (AttributeMetaData amd : emd.getAtomicAttributes())
		{
			result.put(amd, findTags(amd, ontologyIds));
		}
		return result;
	}

	@Override
	public List<OntologyTerm> findTags(AttributeMetaData attribute, List<String> ontologyIds)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		return semanticSearchServiceHelper.findTagsSync(description, ontologyIds);
	}
}
