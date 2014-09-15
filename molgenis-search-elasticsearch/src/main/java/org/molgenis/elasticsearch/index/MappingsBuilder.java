package org.molgenis.elasticsearch.index;

import static org.molgenis.elasticsearch.util.MapperTypeSanitizer.sanitizeMapperType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Range;
import org.molgenis.data.Repository;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.elasticsearch.util.MapperTypeSanitizer;

/**
 * Builds mappings for a documentType. For each column a multi_field is created, one analyzed for searching and one
 * not_analyzed for sorting
 * 
 * @author erwin
 * 
 */
public class MappingsBuilder
{
	private static final Logger logger = Logger.getLogger(MappingsBuilder.class);

	private static final String ENTITY_NAME = "name";
	private static final String ENTITY_LABEL = "label";
	private static final String ENTITY_DESCRIPTION = "description";
	private static final String ENTITY_EXTENDS = "extends";
	private static final String ENTITY_ATTRIBUTES = "attributes";
	private static final String ENTITY_ABSTRACT = "abstract";
	private static final String ENTITY_ENTITY_CLASS = "entityClass";

	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_LABEL = "label";
	private static final String ATTRIBUTE_DESCRIPTION = "description";
	private static final String ATTRIBUTE_RANGE_MAX = "max";
	private static final String ATTRIBUTE_RANGE_MIN = "min";
	private static final String ATTRIBUTE_RANGE = "range";
	private static final String ATTRIBUTE_AGGREGATEABLE = "aggregateable";
	private static final String ATTRIBUTE_ATTRIBUTE_PARTS = "attributeParts";
	private static final String ATTRIBUTE_REF_ENTITY = "refEntity";
	private static final String ATTRIBUTE_AUTO = "auto";
	private static final String ATTRIBUTE_LOOKUP_ATTRIBUTE = "lookupAttribute";
	private static final String ATTRIBUTE_LABEL_ATTRIBUTE = "labelAttribute";
	private static final String ATTRIBUTE_ID_ATTRIBUTE = "idAttribute";
	private static final String ATTRIBUTE_DEFAULT_VALUE = "defaultValue";
	private static final String ATTRIBUTE_VISIBLE = "visible";
	private static final String ATTRIBUTE_UNIQUE = "unique";
	private static final String ATTRIBUTE_READONLY = "readonly";
	private static final String ATTRIBUTE_NILLABLE = "nillable";
	private static final String ATTRIBUTE_DATA_TYPE = "dataType";
	public static final String FIELD_NOT_ANALYZED = "sort";

	/**
	 * Creates entity meta data for the given repository, documents are stored in the index
	 *
	 * @param repository
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder buildMapping(Repository repository) throws IOException
	{
		return buildMapping(repository.getEntityMetaData());
	}

	/**
	 * Creates entity meta data for the given repository
	 *
	 * @param repository
	 * @param storeSource
	 *            whether or not documents are stored in the index
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder buildMapping(Repository repository, boolean storeSource) throws IOException
	{
		return buildMapping(repository.getEntityMetaData(), storeSource);
	}

	/**
	 * Creates a Elasticsearch mapping for the given entity meta data, documents are stored in the index
	 * 
	 * @param entityMetaData
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder buildMapping(EntityMetaData entityMetaData) throws IOException
	{
		return buildMapping(entityMetaData, true);
	}

	/**
	 * Creates a Elasticsearch mapping for the given entity meta data
	 * 
	 * @param entityMetaData
	 * @param storeSource
	 *            whether or not documents are stored in the index
	 * @return
	 * @throws IOException
	 */
	public static XContentBuilder buildMapping(EntityMetaData meta, boolean storeSource) throws IOException
	{
		String documentType = MapperTypeSanitizer.sanitizeMapperType(meta.getName());
		XContentBuilder jsonBuilder = XContentFactory.jsonBuilder().startObject().startObject(documentType)
				.startObject("_source").field("enabled", storeSource).endObject().startObject("properties");

		for (AttributeMetaData attr : meta.getAtomicAttributes())
		{
			String esType = getType(attr);
			if (attr.getDataType().getEnumType().toString().equalsIgnoreCase(MolgenisFieldTypes.MREF.toString()))
			{
				jsonBuilder.startObject(attr.getName()).field("type", "nested").startObject("properties");

				// TODO : what if the attributes in refEntity is also an MREF
				// field?
				for (AttributeMetaData refEntityAttr : attr.getRefEntity().getAttributes())
				{
					if (refEntityAttr.isLabelAttribute())
					{
						jsonBuilder.startObject(refEntityAttr.getName()).field("type", "multi_field")
								.startObject("fields").startObject(refEntityAttr.getName()).field("type", "string")
								.endObject().startObject(FIELD_NOT_ANALYZED).field("type", "string")
								.field("index", "not_analyzed").endObject().endObject().endObject();
					}
					else
					{
						jsonBuilder.startObject(refEntityAttr.getName()).field("type", "string").endObject();
					}
				}
				jsonBuilder.endObject().endObject();
			}
			else if (esType.equals("string"))
			{
				jsonBuilder.startObject(attr.getName()).field("type", "multi_field").startObject("fields")
						.startObject(attr.getName()).field("type", "string").endObject()
						.startObject(FIELD_NOT_ANALYZED).field("type", "string").field("index", "not_analyzed")
						.endObject().endObject().endObject();

			}
			else if (esType.equals("date"))
			{
				String dateFormat;
				if (attr.getDataType().getEnumType() == FieldTypeEnum.DATE) dateFormat = "date"; // yyyy-MM-dd
				else if (attr.getDataType().getEnumType() == FieldTypeEnum.DATE_TIME) dateFormat = "date_time_no_millis"; // yyyy-MM-dd’T’HH:mm:ssZZ
				else
				{
					throw new MolgenisDataException("invalid molgenis field type for elasticsearch date format ["
							+ attr.getDataType().getEnumType() + "]");
				}

				jsonBuilder.startObject(attr.getName()).field("type", "multi_field").startObject("fields")
						.startObject(attr.getName()).field("type", "date").field("format", dateFormat).endObject()
						.startObject(FIELD_NOT_ANALYZED).field("type", "date").field("format", dateFormat).endObject()
						.endObject().endObject();
			}
			else
			{
				jsonBuilder.startObject(attr.getName()).field("type", "multi_field").startObject("fields")
						.startObject(attr.getName()).field("type", esType).endObject().startObject(FIELD_NOT_ANALYZED)
						.field("type", esType).endObject().endObject().endObject();

			}
		}

		jsonBuilder.endObject(); // properties

		// create custom meta data
		jsonBuilder.startObject("_meta");
		serializeEntityMeta(meta, jsonBuilder);
		jsonBuilder.endObject();

		jsonBuilder.endObject().endObject(); // documentType, end

		return jsonBuilder;
	}

	public static void serializeEntityMeta(EntityMetaData entityMetaData, XContentBuilder jsonBuilder)
			throws IOException
	{
		if (entityMetaData.getName() != null) jsonBuilder.field(ENTITY_NAME, entityMetaData.getName());
		jsonBuilder.field(ENTITY_ABSTRACT, entityMetaData.isAbstract());
		if (entityMetaData.getLabel() != null) jsonBuilder.field(ENTITY_LABEL, entityMetaData.getLabel());
		if (entityMetaData.getDescription() != null) jsonBuilder.field(ENTITY_DESCRIPTION,
				entityMetaData.getDescription());
		jsonBuilder.startArray(ENTITY_ATTRIBUTES);
		for (AttributeMetaData attr : entityMetaData.getAttributes())
		{
			serializeAttribute(attr, jsonBuilder);
		}
		jsonBuilder.endArray(); // attributes
		if (entityMetaData.getExtends() != null)
		{
			jsonBuilder.field(ENTITY_EXTENDS, entityMetaData.getExtends().getName());
		}
		if (entityMetaData.getEntityClass() != null)
		{
			jsonBuilder.field(ENTITY_ENTITY_CLASS, entityMetaData.getEntityClass().getName());
		}
	}

	private static void serializeAttribute(AttributeMetaData attr, XContentBuilder jsonBuilder) throws IOException
	{
		jsonBuilder.startObject();
		if (attr.getName() != null) jsonBuilder.field(ATTRIBUTE_NAME, attr.getName());
		if (attr.getLabel() != null) jsonBuilder.field(ATTRIBUTE_LABEL, attr.getLabel());
		if (attr.getDescription() != null) jsonBuilder.field(ATTRIBUTE_DESCRIPTION, attr.getDescription());
		if (attr.getDataType() != null && attr.getDataType().getEnumType() != null)
		{
			jsonBuilder.field(ATTRIBUTE_DATA_TYPE, attr.getDataType().getEnumType());
		}
		jsonBuilder.field(ATTRIBUTE_NILLABLE, attr.isNillable());
		jsonBuilder.field(ATTRIBUTE_READONLY, attr.isReadonly());
		jsonBuilder.field(ATTRIBUTE_UNIQUE, attr.isUnique());
		jsonBuilder.field(ATTRIBUTE_VISIBLE, attr.isVisible());
		// TODO better solution
		if (attr.getDefaultValue() != null) jsonBuilder.field(ATTRIBUTE_DEFAULT_VALUE, attr.getDefaultValue());
		jsonBuilder.field(ATTRIBUTE_ID_ATTRIBUTE, attr.isIdAtrribute());
		jsonBuilder.field(ATTRIBUTE_LABEL_ATTRIBUTE, attr.isLabelAttribute());
		jsonBuilder.field(ATTRIBUTE_LOOKUP_ATTRIBUTE, attr.isLookupAttribute());
		jsonBuilder.field(ATTRIBUTE_AUTO, attr.isAuto());
		if (attr.getRefEntity() != null && attr.getRefEntity().getName() != null)
		{
			jsonBuilder.field(ATTRIBUTE_REF_ENTITY, attr.getRefEntity().getName());
		}
		if (attr.getAttributeParts() != null)
		{
			jsonBuilder.startArray(ATTRIBUTE_ATTRIBUTE_PARTS);
			for (AttributeMetaData attrPart : attr.getAttributeParts())
			{
				serializeAttribute(attrPart, jsonBuilder);
			}
			jsonBuilder.endArray();
		}
		jsonBuilder.field(ATTRIBUTE_AGGREGATEABLE, attr.isAggregateable());
		if (attr.getRange() != null)
		{
			jsonBuilder.startObject(ATTRIBUTE_RANGE);
			if (attr.getRange().getMin() != null) jsonBuilder.field(ATTRIBUTE_RANGE_MIN, attr.getRange().getMin());
			if (attr.getRange().getMax() != null) jsonBuilder.field(ATTRIBUTE_RANGE_MAX, attr.getRange().getMax());
			jsonBuilder.endObject(); // range
		}

		jsonBuilder.endObject();
	}

	@SuppressWarnings("unchecked")
	public static EntityMetaData deserializeEntityMeta(Client client, String entityName) throws IOException
	{
		String docType = sanitizeMapperType(entityName);

		GetMappingsResponse getMappingsResponse = client.admin().indices().prepareGetMappings("molgenis")
				.addTypes(docType).execute().actionGet();
		ImmutableOpenMap<String, MappingMetaData> indexMappings = getMappingsResponse.getMappings().get("molgenis");
		MappingMetaData mappingMetaData = indexMappings.get(docType);
		Map<String, Object> metaMap = (Map<String, Object>) mappingMetaData.sourceAsMap().get("_meta");

		// create entity meta
		String name = (String) metaMap.get(ENTITY_NAME);
		Class<? extends Entity> entityClass;
		try
		{
			entityClass = (Class<? extends Entity>) Class.forName((String) metaMap.get(ENTITY_ENTITY_CLASS));
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		DefaultEntityMetaData entityMetaData = new DefaultEntityMetaData(name, entityClass);

		// deserialize entity meta
		deserializeEntityMeta(metaMap, entityMetaData, client);
		return entityMetaData;
	}

	@SuppressWarnings("unchecked")
	private static void deserializeEntityMeta(Map<String, Object> entityMetaMap, DefaultEntityMetaData entityMetaData,
			Client client) throws IOException
	{
		boolean abstract_ = (boolean) entityMetaMap.get(ENTITY_ABSTRACT);
		entityMetaData.setAbstract(abstract_);

		if (entityMetaMap.containsKey(ENTITY_LABEL))
		{
			String label = (String) entityMetaMap.get(ENTITY_LABEL);
			entityMetaData.setLabel(label);
		}
		if (entityMetaMap.containsKey(ENTITY_DESCRIPTION))
		{
			String description = (String) entityMetaMap.get(ENTITY_DESCRIPTION);
			entityMetaData.setDescription(description);
		}
		if (entityMetaMap.containsKey(ENTITY_ATTRIBUTES))
		{
			List<Map<String, Object>> attributes = (List<Map<String, Object>>) entityMetaMap.get(ENTITY_ATTRIBUTES);
			for (Map<String, Object> attribute : attributes)
			{
				AttributeMetaData attributeMetaData = deserializeAttribute(attribute, entityMetaData, client);
				entityMetaData.addAttributeMetaData(attributeMetaData);
			}
		}
		if (entityMetaMap.containsKey(ENTITY_EXTENDS))
		{
			String extendsEntityName = (String) entityMetaMap.get(ENTITY_EXTENDS);
			EntityMetaData extends_ = deserializeEntityMeta(client, extendsEntityName);
			entityMetaData.setExtends(extends_);
		}
	}

	@SuppressWarnings("unchecked")
	private static AttributeMetaData deserializeAttribute(Map<String, Object> attributeMap,
			DefaultEntityMetaData entityMetaData, Client client) throws IOException
	{
		String name = (String) attributeMap.get(ATTRIBUTE_NAME);
		DefaultAttributeMetaData attribute = new DefaultAttributeMetaData(name);

		if (attributeMap.containsKey(ATTRIBUTE_LABEL))
		{
			attribute.setLabel((String) attributeMap.get(ATTRIBUTE_LABEL));
		}
		if (attributeMap.containsKey(ATTRIBUTE_DESCRIPTION))
		{
			attribute.setDescription((String) attributeMap.get(ATTRIBUTE_DESCRIPTION));
		}
		if (attributeMap.containsKey(ATTRIBUTE_DATA_TYPE))
		{
			String dataType = (String) attributeMap.get(ATTRIBUTE_DATA_TYPE);
			attribute.setDataType(MolgenisFieldTypes.getType(dataType.toLowerCase()));
		}
		attribute.setNillable((boolean) attributeMap.get(ATTRIBUTE_NILLABLE));
		attribute.setReadOnly((boolean) attributeMap.get(ATTRIBUTE_READONLY));
		attribute.setUnique((boolean) attributeMap.get(ATTRIBUTE_UNIQUE));
		attribute.setVisible((boolean) attributeMap.get(ATTRIBUTE_VISIBLE));
		if (attributeMap.containsKey(ATTRIBUTE_DEFAULT_VALUE))
		{
			// TODO convert default value to correct molgenis type
			attribute.setDefaultValue(attributeMap.get(ATTRIBUTE_DEFAULT_VALUE));
		}
		attribute.setIdAttribute((boolean) attributeMap.get(ATTRIBUTE_ID_ATTRIBUTE));
		attribute.setLabelAttribute((boolean) attributeMap.get(ATTRIBUTE_LABEL_ATTRIBUTE));
		attribute.setLookupAttribute((boolean) attributeMap.get(ATTRIBUTE_LOOKUP_ATTRIBUTE));
		attribute.setAuto((boolean) attributeMap.get(ATTRIBUTE_AUTO));
		if (attributeMap.containsKey(ATTRIBUTE_REF_ENTITY))
		{
			// TODO use dataservice to retrieve ref entity meta data instead of this repo
			String refEntityName = (String) attributeMap.get(ATTRIBUTE_REF_ENTITY);
			EntityMetaData refEntityMeta = deserializeEntityMeta(client, refEntityName);
			attribute.setRefEntity(refEntityMeta);
		}
		if (attributeMap.containsKey(ATTRIBUTE_ATTRIBUTE_PARTS))
		{
			List<Map<String, Object>> attributeParts = (List<Map<String, Object>>) attributeMap
					.get(ATTRIBUTE_ATTRIBUTE_PARTS);

			List<AttributeMetaData> attributeMetaDataParts = new ArrayList<AttributeMetaData>();
			for (Map<String, Object> attributePart : attributeParts)
			{
				AttributeMetaData attributeMetaDataPart = deserializeAttribute(attributePart, entityMetaData, client);
				attributeMetaDataParts.add(attributeMetaDataPart);
			}
			attribute.setAttributesMetaData(attributeMetaDataParts);
		}
		attribute.setAggregateable((boolean) attributeMap.get(ATTRIBUTE_AGGREGATEABLE));
		if (attributeMap.containsKey(ATTRIBUTE_RANGE))
		{
			Map<String, Object> range = (Map<String, Object>) attributeMap.get(ATTRIBUTE_RANGE);
			Long min = range.containsKey(ATTRIBUTE_RANGE_MIN) ? (Long) range.get(ATTRIBUTE_RANGE_MIN) : null;
			Long max = range.containsKey(ATTRIBUTE_RANGE_MAX) ? (Long) range.get(ATTRIBUTE_RANGE_MAX) : null;
			attribute.setRange(new Range(min, max));
		}
		return attribute;

	}

	private static String getType(AttributeMetaData attr)
	{
		FieldTypeEnum enumType = attr.getDataType().getEnumType();
		switch (enumType)
		{
			case BOOL:
				return "boolean";
			case DATE:
			case DATE_TIME:
				return "date";
			case DECIMAL:
				return "double";
			case INT:
				return "integer";
			case LONG:
				return "long";
			case CATEGORICAL:
			case EMAIL:
			case ENUM:
			case HTML:
			case HYPERLINK:
			case STRING:
			case TEXT:
				return "string";
			case MREF:
			case XREF:
			{
				// return type of referenced label field
                //FIXME hack for vcf's
                if(attr.getRefEntity()!=null) {
                    return getType(attr.getRefEntity().getLabelAttribute());
                }
                return "string";
			}
			case FILE:
			case IMAGE:
				throw new ElasticsearchException("indexing of molgenis field type [" + enumType + "] not supported");
			default:
				return "string";
		}
	}

	public static boolean hasMapping(Client client, EntityMetaData entityMetaData, String indexName)
	{
		String docType = sanitizeMapperType(entityMetaData.getName());

		GetMappingsResponse getMappingsResponse = client.admin().indices().prepareGetMappings(indexName).execute()
				.actionGet();
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> allMappings = getMappingsResponse
				.getMappings();
		final ImmutableOpenMap<String, MappingMetaData> indexMappings = allMappings.get(indexName);
		return indexMappings.containsKey(docType);
	}

	public static void createMapping(Client client, EntityMetaData entityMetaData, String indexName) throws IOException
	{
		XContentBuilder jsonBuilder = MappingsBuilder.buildMapping(entityMetaData);

		String docType = sanitizeMapperType(entityMetaData.getName());
		PutMappingResponse response = client.admin().indices().preparePutMapping(indexName).setType(docType)
				.setSource(jsonBuilder).execute().actionGet();

		if (!response.isAcknowledged())
		{
			throw new ElasticsearchException("Creation of mapping for documentType [" + docType + "] failed. Response="
					+ response);
		}

		logger.info("Mapping for documentType [" + docType + "] created");
	}
}
