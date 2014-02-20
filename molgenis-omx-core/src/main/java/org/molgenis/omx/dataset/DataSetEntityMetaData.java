package org.molgenis.omx.dataset;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.support.AbstractEntityMetaData;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.protocol.ProtocolEntityMetaData;

public class DataSetEntityMetaData extends AbstractEntityMetaData
{
	private final DataSet dataSet;

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
		return new ProtocolEntityMetaData(dataSet.getProtocolUsed()).getAttributes();
	}

	@Override
	public Iterable<AttributeMetaData> getAtomicAttributes()
	{
		return new ProtocolEntityMetaData(dataSet.getProtocolUsed()).getAtomicAttributes();
	}
}
