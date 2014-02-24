package org.molgenis.data.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

public class DefaultEntityMetaData implements EntityMetaData
{
	private final String name;
	private String label;
	private String description;
	private final Map<String, AttributeMetaData> attributes = new LinkedHashMap<String, AttributeMetaData>();

	public DefaultEntityMetaData(String name)
	{
		if (name == null) throw new IllegalArgumentException("Name cannot be null");
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void addAttributeMetaData(AttributeMetaData attributeMetaData)
	{
		if (attributeMetaData == null) throw new IllegalArgumentException("AttributeMetaData cannot be null");
		if (attributeMetaData.getName() == null) throw new IllegalArgumentException(
				"Name of the AttributeMetaData cannot be null");

		attributes.put(attributeMetaData.getName().toLowerCase(), attributeMetaData);
	}

	@Override
	public List<AttributeMetaData> getAttributes()
	{
		return Collections.unmodifiableList(new ArrayList<AttributeMetaData>(attributes.values()));
	}

	@Override
	public AttributeMetaData getIdAttribute()
	{
		for (AttributeMetaData attribute : attributes.values())
		{
			if (attribute.isIdAtrribute())
			{
				return attribute;
			}
		}

		return null;
	}

	@Override
	public List<AttributeMetaData> getLabelAttributes()
	{
		Collection<AttributeMetaData> labels = Collections2.filter(attributes.values(),
				new Predicate<AttributeMetaData>()
				{
					@Override
					public boolean apply(AttributeMetaData attr)
					{
						return attr.isLabelAttribute();
					}

				});

		return labels.isEmpty() ? Arrays.asList(getIdAttribute()) : Lists.newArrayList(labels);
	}

	@Override
	public AttributeMetaData getAttribute(String attributeName)
	{
		if (attributeName == null) throw new IllegalArgumentException("AttributeName is null");
		return attributes.get(attributeName.toLowerCase());
	}

	@Override
	public String getLabel()
	{
		return label != null ? label : name;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DefaultEntityMetaData other = (DefaultEntityMetaData) obj;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		return true;
	}

}
