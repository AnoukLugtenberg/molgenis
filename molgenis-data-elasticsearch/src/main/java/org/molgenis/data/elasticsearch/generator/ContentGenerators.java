package org.molgenis.data.elasticsearch.generator;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.Sort;
import org.molgenis.data.elasticsearch.generator.model.Index;
import org.molgenis.data.elasticsearch.generator.model.Mapping;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
public class ContentGenerators
{
	private final IndexGenerator indexGenerator;
	private final MappingGenerator mappingGenerator;
	private final QueryContentGenerators queryGenerators;
	private final DocumentContentBuilder documentGenerator;

	public ContentGenerators(IndexGenerator indexGenerator, MappingGenerator mappingGenerator,
			QueryContentGenerators queryGenerators, DocumentContentBuilder documentGenerator)
	{
		this.indexGenerator = requireNonNull(indexGenerator);
		this.mappingGenerator = requireNonNull(mappingGenerator);
		this.queryGenerators = requireNonNull(queryGenerators);
		this.documentGenerator = requireNonNull(documentGenerator);
	}

	public Index createIndex(EntityType entityType)
	{
		return indexGenerator.createIndex(entityType);
	}

	public Mapping createMapping(EntityType entityType)
	{
		return mappingGenerator.createMapping(entityType);
	}

	public QueryBuilder createQuery(Query<Entity> query, EntityType entityType)
	{
		return queryGenerators.createQuery(query, entityType);
	}

	public List<SortBuilder> createSorts(Sort sort, EntityType entityType)
	{
		return queryGenerators.createSorts(sort, entityType);
	}

	public List<AggregationBuilder> createAggregations(Attribute aggAttr1, Attribute aggAttr2,
			Attribute aggAttrDistinct)
	{
		return queryGenerators.createAggregations(aggAttr1, aggAttr2, aggAttrDistinct);
	}

	public XContentBuilder createDocument(Entity entity)
	{
		return documentGenerator.createDocument(entity);
	}
}
