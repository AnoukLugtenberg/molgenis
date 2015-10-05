package org.molgenis.data.rest.v2;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.rest.v2.RestControllerV2.BASE_URI;
import static org.molgenis.util.MolgenisDateFormat.getDateFormat;
import static org.molgenis.util.MolgenisDateFormat.getDateTimeFormat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AggregateQuery;
import org.molgenis.data.AggregateResult;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisQueryException;
import org.molgenis.data.Query;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.rest.EntityPager;
import org.molgenis.data.rest.Href;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.file.FileMeta;
import org.molgenis.security.core.MolgenisPermissionService;
import org.molgenis.util.ErrorMessageResponse;
import org.molgenis.util.ErrorMessageResponse.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(BASE_URI)
class RestControllerV2
{
	private static final Logger LOG = LoggerFactory.getLogger(RestControllerV2.class);

	public static final String BASE_URI = "/api/v2";

	private final DataService dataService;
	private final MolgenisPermissionService permissionService;

	@Autowired
	public RestControllerV2(DataService dataService, MolgenisPermissionService permissionService)
	{
		this.dataService = requireNonNull(dataService);
		this.permissionService = requireNonNull(permissionService);
	}

	/**
	 * Retrieve an entity instance by id, optionally specify which attributes to include in the response.
	 * 
	 * @param entityName
	 * @param id
	 * @param attributeFilter
	 * @return
	 */
	@RequestMapping(value = "/{entityName}/{id:.+}", method = GET)
	@ResponseBody
	public Map<String, Object> retrieveEntity(@PathVariable("entityName") String entityName,
			@PathVariable("id") Object id,
			@RequestParam(value = "attrs", required = false) AttributeFilter attributeFilter)
	{
		Entity entity = dataService.findOne(entityName, id);
		if (entity == null)
		{
			throw new UnknownEntityException(entityName + " [" + id + "] not found");
		}

		return createEntityResponse(entity, attributeFilter, true);
	}

	@RequestMapping(value = "/{entityName}/{id:.+}", method = POST, params = "_method=GET")
	@ResponseBody
	public Map<String, Object> retrieveEntityPost(@PathVariable("entityName") String entityName,
			@PathVariable("id") Object id,
			@RequestParam(value = "attrs", required = false) AttributeFilter attributeFilter)
	{
		Entity entity = dataService.findOne(entityName, id);
		if (entity == null)
		{
			throw new UnknownEntityException(entityName + " [" + id + "] not found");
		}

		return createEntityResponse(entity, attributeFilter, true);
	}

	@RequestMapping(value = "/{entityName}/{id:.+}", method = DELETE)
	@ResponseStatus(NO_CONTENT)
	public void deleteEntity(@PathVariable("entityName") String entityName, @PathVariable("id") Object id)
	{
		dataService.delete(entityName, id);
	}

	/**
	 * Retrieve an entity collection, optionally specify which attributes to include in the response.
	 * 
	 * @param entityName
	 * @param request
	 * @param attributes
	 * @return
	 */
	@RequestMapping(value = "/{entityName}", method = GET)
	@ResponseBody
	public EntityCollectionResponseV2 retrieveEntityCollection(@PathVariable("entityName") String entityName,
			@Valid EntityCollectionRequestV2 request)
	{
		return createEntityCollectionResponse(entityName, request);
	}

	@RequestMapping(value = "/{entityName}", method = POST, params = "_method=GET")
	@ResponseBody
	public EntityCollectionResponseV2 retrieveEntityCollectionPost(@PathVariable("entityName") String entityName,
			@Valid EntityCollectionRequestV2 request)
	{
		return createEntityCollectionResponse(entityName, request);
	}

	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ErrorMessageResponse handleRuntimeException(RuntimeException e)
	{
		LOG.error("", e);
		return new ErrorMessageResponse(new ErrorMessage(e.getMessage()));
	}

	private EntityCollectionResponseV2 createEntityCollectionResponse(String entityName,
			EntityCollectionRequestV2 request)
	{
		EntityMetaData meta = dataService.getEntityMetaData(entityName);

		Query q = request.getQ() != null ? request.getQ().createQuery(meta) : new QueryImpl();
		q.pageSize(request.getNum()).offset(request.getStart()).sort(request.getSort());

		if (request.getAggs() != null)
		{
			// return aggregates for aggregate query
			AggregateQuery aggsQ = request.getAggs().createAggregateQuery(meta, q);
			if (aggsQ.getAttributeX() == null && aggsQ.getAttributeY() == null)
			{
				throw new MolgenisQueryException("Aggregate query is missing 'x' or 'y' attribute");
			}
			AggregateResult aggs = dataService.aggregate(entityName, aggsQ);
			return new EntityAggregatesResponse(aggs, BASE_URI + '/' + entityName);
		}
		else
		{
			// return entities for query
			Iterable<Entity> it = dataService.findAll(entityName, q);
			Long count = dataService.count(entityName, q);
			EntityPager pager = new EntityPager(request.getStart(), request.getNum(), count, it);

			AttributeFilter attributeFilter = request.getAttrs();
			if (attributeFilter == null)
			{
				attributeFilter = AttributeFilter.ALL_ATTRS_FILTER;
			}
			List<Map<String, Object>> entities = new ArrayList<>();
			createEntitiesValuesResponse(it, meta, attributeFilter, entities);

			return new EntityCollectionResponseV2(pager, entities, attributeFilter, BASE_URI + '/' + entityName, meta,
					permissionService);
		}
	}

	private void createEntitiesValuesResponse(Iterable<Entity> entities, EntityMetaData entityMeta,
			AttributeFilter attrFilter, List<Map<String, Object>> entitiesValues)
	{
		// create value maps, keep track of ref attributes and entities that need to be resolved
		Map<AttributeMetaData, Set<Object>> refEntitiesIds = new HashMap<>();

		for (Entity entity : entities)
		{
			Map<String, Object> entityValues = new LinkedHashMap<>();
			for (AttributeMetaData attr : entityMeta.getAttributes())
			{
				if (attrFilter.includeAttribute(attr))
				{
					createEntityAttrValues(entity, attr, entityValues, refEntitiesIds);
				}
			}
			entitiesValues.add(entityValues);
		}

		// retrieve ref entities
		refEntitiesIds.forEach((refAttr, refEntityIds) -> {
			EntityMetaData refEntityMeta = refAttr.getRefEntity();
			String refEntityName = refEntityMeta.getName();
			Iterable<Entity> refEntities = dataService.findAll(refEntityName, refEntityIds);
			Map<Object, Entity> refEntitiesMap = StreamSupport.stream(refEntities.spliterator(), false)
					.collect(Collectors.toMap(Entity::getIdValue, Function.identity()));

			// update value maps
			String refAttrName = refAttr.getName();
			FieldTypeEnum attrType = refAttr.getDataType().getEnumType();
			entitiesValues.forEach(entityValue -> {
				Object refAttrValue = entityValue.get(refAttrName);
				switch (attrType)
				{
					case CATEGORICAL:
					case XREF:
					case FILE:
						if (refAttrValue != null)
						{
							Entity attrRefEntity = refEntitiesMap.get(refAttrValue);
							List<Map<String, Object>> refEntitiesValues = new ArrayList<>();
							AttributeFilter refAttrFilter = attrFilter.getAttributeFilter(refAttr);
							if (refAttrFilter == null)
							{
								refAttrFilter = createDefaultRefAttributeFilter(refAttr);
							}
							createEntitiesValuesResponse(Arrays.asList(attrRefEntity), refEntityMeta, refAttrFilter,
									refEntitiesValues);
							entityValue.put(refAttrName, refEntitiesValues.get(0));
						}
						break;
					case CATEGORICAL_MREF:
					case MREF:
						if (refAttrValue != null)
						{
							Iterable<Entity> attrRefEntities = ((List<Object>) refAttrValue).stream()
									.map(refEntitiesMap::get).collect(Collectors.toList());
							List<Map<String, Object>> mrefEntitiesValues = new ArrayList<>();
							AttributeFilter refAttrFilter = attrFilter.getAttributeFilter(refAttr);
							if (refAttrFilter == null)
							{
								refAttrFilter = createDefaultRefAttributeFilter(refAttr);
							}
							createEntitiesValuesResponse(attrRefEntities, refEntityMeta, refAttrFilter,
									mrefEntitiesValues);
							entityValue.put(refAttrName, mrefEntitiesValues);
						}
						break;
					// $CASES-OMITTED$
					default:
						throw new RuntimeException("Unknown data type [" + attrType + "]");
				}
			});
		});
	}

	private void createEntityAttrValues(Entity entity, AttributeMetaData attr, Map<String, Object> entityValues,
			Map<AttributeMetaData, Set<Object>> refEntitiesIds)
	{
		String attrName = attr.getName();
		FieldTypeEnum attrType = attr.getDataType().getEnumType();
		switch (attrType)
		{
			case BOOL:
				entityValues.put(attrName, entity.getBoolean(attrName));
				break;
			case CATEGORICAL:
			case XREF:
			case FILE:
				Entity refEntity = entity.getEntity(attrName);
				Object value;
				if (refEntity != null)
				{
					value = refEntity.getIdValue();

					// bookkeeping: keep track of referred entity ids that will be resolved later
					Set<Object> xrefEntityIds = refEntitiesIds.get(attr);
					if (xrefEntityIds == null)
					{
						xrefEntityIds = new HashSet<>();
						refEntitiesIds.put(attr, xrefEntityIds);
					}
					xrefEntityIds.add(value);
				}
				else
				{
					value = null;
				}
				entityValues.put(attrName, value);
				break;
			case CATEGORICAL_MREF:
			case MREF:
				Iterable<Entity> refEntities = entity.getEntities(attrName);
				List<Object> refEntityIds = StreamSupport.stream(refEntities.spliterator(), false)
						.map(Entity::getIdValue).collect(Collectors.toList());
				entityValues.put(attrName, refEntityIds);

				// bookkeeping: keep track of referred entity ids that will be resolved later
				Set<Object> mrefEntityIds = refEntitiesIds.get(attr);
				if (mrefEntityIds == null)
				{
					mrefEntityIds = new HashSet<>();
					refEntitiesIds.put(attr, mrefEntityIds);
				}
				mrefEntityIds.addAll(refEntityIds);
				break;
			case COMPOUND:
				Iterable<AttributeMetaData> attrParts = attr.getAttributeParts();
				attrParts.forEach(attrPart -> createEntityAttrValues(entity, attrPart, entityValues, refEntitiesIds));
				break;
			case DATE:
				Date dateValue = entity.getDate(attrName);
				String dateValueStr = dateValue != null ? getDateFormat().format(dateValue) : null;
				entityValues.put(attrName, dateValueStr);
				break;
			case DATE_TIME:
				Date dateTimeValue = entity.getDate(attrName);
				String dateTimeValueStr = dateTimeValue != null ? getDateTimeFormat().format(dateTimeValue) : null;
				entityValues.put(attrName, dateTimeValueStr);
				break;
			case DECIMAL:
				entityValues.put(attrName, entity.getDouble(attrName));
				break;
			case EMAIL:
			case ENUM:
			case HTML:
			case HYPERLINK:
			case SCRIPT:
			case STRING:
			case TEXT:
				entityValues.put(attrName, entity.getString(attrName));
				break;
			case IMAGE:
				throw new UnsupportedOperationException("Unsupported attribute type [" + attrType + "]");
			case INT:
				entityValues.put(attrName, entity.getInt(attrName));
				break;
			case LONG:
				entityValues.put(attrName, entity.getLong(attrName));
				break;
			default:
				throw new RuntimeException("Unknown data type [" + attrType + "]");
		}
	}

	private Map<String, Object> createEntityResponse(Entity entity, AttributeFilter attrFilter, boolean includeMetaData)
	{
		Map<String, Object> responseData = new LinkedHashMap<String, Object>();
		if (includeMetaData)
		{
			createEntityMetaResponse(entity.getEntityMetaData(), attrFilter, responseData);
		}
		createEntityValuesResponse(entity, attrFilter, responseData);
		return responseData;
	}

	private void createEntityMetaResponse(EntityMetaData entityMetaData, AttributeFilter attrFilter,
			Map<String, Object> responseData)
	{
		responseData.put("_meta", new EntityMetaDataResponseV2(entityMetaData, attrFilter, permissionService));
	}

	private void createEntityValuesResponse(Entity entity, AttributeFilter attrFilter, Map<String, Object> responseData)
	{
		Iterable<AttributeMetaData> attrs = entity.getEntityMetaData().getAttributes();
		attrFilter = attrFilter != null ? attrFilter : AttributeFilter.ALL_ATTRS_FILTER;
		createEntityValuesResponseRec(entity, attrs, attrFilter, responseData);
	}

	private void createEntityValuesResponseRec(Entity entity, Iterable<AttributeMetaData> attrs,
			AttributeFilter attrFilter, Map<String, Object> responseData)
	{
		responseData.put("_href",
				Href.concatEntityHref(BASE_URI, entity.getEntityMetaData().getName(), entity.getIdValue()));
		for (AttributeMetaData attr : attrs)
		{
			String attrName = attr.getName();
			if (attrFilter.includeAttribute(attr))
			{
				FieldTypeEnum dataType = attr.getDataType().getEnumType();
				switch (dataType)
				{
					case BOOL:
						responseData.put(attrName, entity.getBoolean(attrName));
						break;
					case CATEGORICAL:
					case XREF:
					case FILE:
						Entity refEntity = entity.getEntity(attrName);
						Map<String, Object> refEntityResponse;
						if (refEntity != null)
						{
							AttributeFilter refAttrFilter = attrFilter.getAttributeFilter(attr);
							if (refAttrFilter == null)
							{
								refAttrFilter = createDefaultRefAttributeFilter(attr);
							}
							refEntityResponse = createEntityResponse(refEntity, refAttrFilter, false);
						}
						else
						{
							refEntityResponse = null;
						}
						responseData.put(attrName, refEntityResponse);
						break;
					case CATEGORICAL_MREF:
					case MREF:
						Iterable<Entity> refEntities = entity.getEntities(attrName);
						List<Map<String, Object>> refEntityResponses;
						if (refEntities != null)
						{
							refEntityResponses = new ArrayList<Map<String, Object>>();
							AttributeFilter refAttrFilter = attrFilter.getAttributeFilter(attr);
							if (refAttrFilter == null)
							{
								refAttrFilter = createDefaultRefAttributeFilter(attr);
							}
							for (Entity refEntitiesEntity : refEntities)
							{
								refEntityResponses.add(createEntityResponse(refEntitiesEntity, refAttrFilter, false));
							}
						}
						else
						{
							refEntityResponses = null;
						}
						responseData.put(attrName, refEntityResponses);
						break;
					case COMPOUND:
						Iterable<AttributeMetaData> attrParts = attr.getAttributeParts();
						AttributeFilter compoundAttrFilter = new AttributeFilter();
						for (AttributeMetaData attrPart : attrParts)
						{
							compoundAttrFilter.add(attrPart.getName());
						}
						createEntityValuesResponseRec(entity, attrParts, compoundAttrFilter, responseData);
						break;
					case DATE:
						Date dateValue = entity.getDate(attrName);
						String dateValueStr = dateValue != null ? getDateFormat().format(dateValue) : null;
						responseData.put(attrName, dateValueStr);
						break;
					case DATE_TIME:
						Date dateTimeValue = entity.getDate(attrName);
						String dateTimeValueStr = dateTimeValue != null ? getDateTimeFormat().format(dateTimeValue)
								: null;
						responseData.put(attrName, dateTimeValueStr);
						break;
					case DECIMAL:
						responseData.put(attrName, entity.getDouble(attrName));
						break;
					case EMAIL:
					case ENUM:
					case HTML:
					case HYPERLINK:
					case SCRIPT:
					case STRING:
					case TEXT:
						responseData.put(attrName, entity.getString(attrName));
						break;
					case IMAGE:
						throw new UnsupportedOperationException("Unsupported data type [" + dataType + "]");
					case INT:
						responseData.put(attrName, entity.getInt(attrName));
						break;
					case LONG:
						responseData.put(attrName, entity.getLong(attrName));
						break;
					default:
						throw new RuntimeException("Unknown data type [" + dataType + "]");
				}
			}
		}
	}

	static AttributeFilter createDefaultRefAttributeFilter(AttributeMetaData attr)
	{
		EntityMetaData refEntityMeta = attr.getRefEntity();
		String idAttrName = refEntityMeta.getIdAttribute().getName();
		String labelAttrName = refEntityMeta.getLabelAttribute().getName();
		AttributeFilter attrFilter = new AttributeFilter().add(idAttrName).add(labelAttrName);
		if (attr.getDataType().getEnumType() == FieldTypeEnum.FILE)
		{
			attrFilter.add(FileMeta.URL);
		}
		return attrFilter;
	}
}
