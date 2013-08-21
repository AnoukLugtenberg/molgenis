package org.molgenis.omx.ontologyMatcher.plugin;

import java.util.ArrayList;
import java.util.List;

import org.molgenis.omx.observ.DataSet;

public class OntologyMatcherModel
{
	List<DataSet> dataSets = new ArrayList<DataSet>();

	DataSet selectedDataSet = null;

	private String url = null;

	public DataSet getSelectedDataSet()
	{
		return selectedDataSet;
	}

	public void setSelectedDataSet(DataSet selectedDataSet)
	{
		this.selectedDataSet = selectedDataSet;
	}

	public List<DataSet> getDataSets()
	{
		return dataSets;
	}

	public void setDataSets(List<DataSet> dataSets)
	{
		this.dataSets = dataSets;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getUrl()
	{
		return this.url;
	}
}