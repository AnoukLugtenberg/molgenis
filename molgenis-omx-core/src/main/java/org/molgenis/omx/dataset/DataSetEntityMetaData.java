package org.molgenis.omx.dataset;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EditableEntityMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Package;
import org.molgenis.data.support.AbstractEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.protocol.ProtocolEntityMetaData;

public class DataSetEntityMetaData extends AbstractEntityMetaData
{
	private final DataSet dataSet;
	private transient Iterable<AttributeMetaData> cachedAttributes;
	private transient Iterable<AttributeMetaData> cachedAtomicAttributes;

	public DataSetEntityMetaData(DataSet dataSet)
	{
		if (dataSet == null) throw new IllegalArgumentException("DataSet is null");
		this.dataSet = dataSet;
	}

	@Override
	public String getName()
	{
		return dataSet.getIdentifier(); // yes, getIdentifier and not getName
	}

	@Override
	public boolean isAbstract()
	{
		return false;
	}

	@Override
	public String getLabel()
	{
		return dataSet.getName(); // yes, getName
	}

	@Override
	public String getDescription()
	{
		return dataSet.getDescription();
	}

	@Override
	public Iterable<AttributeMetaData> getAttributes()
	{
		if (cachedAttributes == null)
		{
			cachedAttributes = new ProtocolEntityMetaData(dataSet.getProtocolUsed()).getAttributes();
		}
		return cachedAttributes;
	}

	@Override
	public Iterable<AttributeMetaData> getAtomicAttributes()
	{
		if (cachedAtomicAttributes == null)
		{
			cachedAtomicAttributes = new ProtocolEntityMetaData(dataSet.getProtocolUsed()).getAtomicAttributes();
		}
		return cachedAtomicAttributes;
	}

	@Override
	public EntityMetaData getExtends()
	{
		return null;
	}

	@Override
	public Class<? extends Entity> getEntityClass()
	{
		return MapEntity.class;
	}

	@Override
	public Package getPackage()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSimpleName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EditableEntityMetaData setLabel(String string)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EditableEntityMetaData setDescription(String string)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EditableEntityMetaData setExtends(EntityMetaData extendsEntityMeta)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EditableEntityMetaData setPackage(Package packageImpl)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EditableEntityMetaData setAbstract(boolean boolean1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAttributeMetaData(AttributeMetaData attributeMetaData)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public AttributeMetaData addAttribute(String string)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
