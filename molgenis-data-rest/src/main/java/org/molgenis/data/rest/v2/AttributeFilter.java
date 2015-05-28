package org.molgenis.data.rest.v2;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class AttributeFilter implements Iterable<AttributeFilter>
{
	public static final AttributeFilter ALL_ATTRS_FILTER = new AttributeFilter().setIncludeAllAttrs(true);

	private final Map<String, AttributeFilter> attributes;
	private boolean includeAllAttrs;

	public AttributeFilter()
	{
		this.attributes = new LinkedHashMap<String, AttributeFilter>();
	}

	public boolean isIncludeAllAttrs()
	{
		return includeAllAttrs;
	}

	AttributeFilter setIncludeAllAttrs(boolean includeAllAttrs)
	{
		this.includeAllAttrs = includeAllAttrs;
		return this;
	}

	public AttributeFilter getAttributeFilter(String name)
	{
		return attributes.get(normalize(name));

	}

	public boolean includeAttribute(String name)
	{
		return this.isIncludeAllAttrs() ? true : attributes.containsKey(normalize(name));
	}

	@Override
	public Iterator<AttributeFilter> iterator()
	{
		return attributes.values().iterator();
	}

	public AttributeFilter add(String name)
	{
		return add(name, null);
	}

	public AttributeFilter add(String name, AttributeFilter attributeSelection)
	{
		attributes.put(normalize(name), attributeSelection);
		return this;
	}

	private String normalize(String name)
	{
		return name.toLowerCase();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + (includeAllAttrs ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AttributeFilter other = (AttributeFilter) obj;
		if (attributes == null)
		{
			if (other.attributes != null) return false;
		}
		else if (!attributes.equals(other.attributes)) return false;
		if (includeAllAttrs != other.includeAllAttrs) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "AttributeFilter [attributes=" + attributes + ", includeAllAttrs=" + includeAllAttrs + "]";
	}
}