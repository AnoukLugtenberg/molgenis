package org.molgenis.data.support;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.Entity;
import org.molgenis.data.convert.DateToStringConverter;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.EntityMetaData;

public class EntityImpl implements Entity
{
	private static final long serialVersionUID = 1L;

	// TODO add final modifier
	/**
	 * Entity meta data
	 */
	private EntityMetaData entityMeta;

	// TODO add final modifier
	/**
	 * Maps attribute names to values. Value class types are determined by attribute data type.
	 */
	private Map<String, Object> values;

	// TODO remove constructor
	protected EntityImpl()
	{
		this.values = newHashMap();
	}

	/**
	 * Constructs an entity with the given entity meta data.
	 *
	 * @param entityMeta entity meta
	 */
	public EntityImpl(EntityMetaData entityMeta)
	{
		this.entityMeta = requireNonNull(entityMeta);
		this.values = newHashMap();
		// FIXME initialize values with hashmap with expected size (at the moment results in NPE)
		//this.values = newHashMapWithExpectedSize(Iterables.size(entityMeta.getAtomicAttributes()));
	}

	// TODO remove method
	protected void init(EntityMetaData entityMeta)
	{
		this.entityMeta = requireNonNull(entityMeta);
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		return entityMeta; // TODO should we return immutable meta data?
	}

	// TODO remove, use getEntityMetaData to retrieve entity meta data
	@Override
	public Iterable<String> getAttributeNames()
	{
		return stream(entityMeta.getAtomicAttributes().spliterator(), false).map(AttributeMetaData::getName)::iterator;
	}

	// TODO getIdValue should return id of type of entity (add Class<P> getIdType() on EntityMetaData?)
	@Override
	public Object getIdValue()
	{
		// abstract entities might not have an id attribute
		AttributeMetaData idAttr = entityMeta.getIdAttribute();
		return idAttr != null ? get(idAttr.getName()) : null;
	}

	@Override
	public void setIdValue(Object id)
	{
		AttributeMetaData idAttr = entityMeta.getIdAttribute();
		if (idAttr == null)
		{
			throw new IllegalArgumentException(format("Entity [%s] doesn't have an id attribute"));
		}
		set(idAttr.getName(), id);
	}

	// TODO getLabelValue should return Object
	@Override
	public String getLabelValue()
	{
		// abstract entities might not have an label attribute
		AttributeMetaData labelAttr = entityMeta.getLabelAttribute();
		return labelAttr != null ? getLabelValueAsString(labelAttr) : null;
	}

	@Override
	public Object get(String attrName)
	{
		return values.get(attrName);
	}

	@Override
	public String getString(String attrName)
	{
		return (String) get(attrName);
	}

	@Override
	public Integer getInt(String attrName)
	{
		return (Integer) get(attrName);
	}

	@Override
	public Long getLong(String attrName)
	{
		return (Long) get(attrName);
	}

	@Override
	public Boolean getBoolean(String attrName)
	{
		return (Boolean) get(attrName);
	}

	@Override
	public Double getDouble(String attrName)
	{
		return (Double) get(attrName);
	}

	@Override
	public Date getDate(String attrName)
	{
		Object value = get(attrName);
		return value != null ? new java.sql.Date(((java.util.Date) value).getTime()) : null;
	}

	@Override
	public java.util.Date getUtilDate(String attrName)
	{
		Object value = get(attrName);
		return value != null ? new java.util.Date(((java.util.Date) value).getTime()) : null;
	}

	@Override
	public Timestamp getTimestamp(String attrName)
	{
		Object value = get(attrName);
		return value != null ? new java.sql.Timestamp(((java.util.Date) value).getTime()) : null;
	}

	@Override
	public Entity getEntity(String attrName)
	{
		return (Entity) get(attrName);
	}

	@Override
	public <E extends Entity> E getEntity(String attrName, Class<E> clazz)
	{
		return (E) get(attrName);
	}

	@Override
	public Iterable<Entity> getEntities(String attrName)
	{
		Object value = get(attrName);
		return value != null ? (Iterable<Entity>) value : emptyList();
	}

	@Override
	public <E extends Entity> Iterable<E> getEntities(String attrName, Class<E> clazz)
	{
		Object value = get(attrName);
		return value != null ? (Iterable<E>) value : emptyList();
	}

	// TODO remove method, move to utility class
	@Override
	public List<String> getList(String attrName)
	{
		throw new RuntimeException("TODO implement");
	}

	// TODO remove method, move to utility class
	@Override
	public List<Integer> getIntList(String attrName)
	{
		throw new RuntimeException("TODO implement");
	}

	@Override
	public void set(String attrName, Object value)
	{
		values.put(attrName, value);
	}

	// TODO remove method, move to utility class
	@Override
	public void set(Entity values)
	{
		throw new RuntimeException("TODO implement");
	}

	private String getLabelValueAsString(AttributeMetaData labelAttr)
	{
		String labelAttributeName = labelAttr.getName();
		MolgenisFieldTypes.FieldTypeEnum dataType = labelAttr.getDataType().getEnumType();
		switch (dataType)
		{
			case BOOL:
			case DECIMAL:
			case EMAIL:
			case ENUM:
			case HTML:
			case HYPERLINK:
			case INT:
			case LONG:
			case SCRIPT:
			case STRING:
			case TEXT:
				Object obj = get(labelAttributeName);
				return obj != null ? obj.toString() : null;
			case DATE:
			case DATE_TIME:
				java.util.Date date = getUtilDate(labelAttributeName);
				return new DateToStringConverter().convert(date);
			case CATEGORICAL:
			case XREF:
			case FILE:
				Entity refEntity = getEntity(labelAttributeName);
				return refEntity != null ? refEntity.getLabelValue() : null;
			case CATEGORICAL_MREF:
			case MREF:
				Iterable<Entity> refEntities = getEntities(labelAttributeName);
				if (refEntities != null)
				{
					StringBuilder strBuilder = new StringBuilder();
					for (Entity mrefEntity : refEntities)
					{
						if (strBuilder.length() > 0) strBuilder.append(',');
						strBuilder.append(mrefEntity.getLabelValue());
					}
					return strBuilder.toString();
				}
				return null;
			case COMPOUND:
				throw new RuntimeException("invalid label data type " + dataType);
			default:
				throw new RuntimeException("unsupported label data type " + dataType);
		}
	}
}
