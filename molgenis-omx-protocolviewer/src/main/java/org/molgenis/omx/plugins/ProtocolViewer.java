package org.molgenis.omx.plugins;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.molgenis.omx.plugins.ProtocolViewerController.JSDataSet;

/**
 * Protocol viewer model
 */
public class ProtocolViewer implements Serializable
{
	private static final long serialVersionUID = 1L;

	private List<JSDataSet> dataSets;
	
	private String ShowViewButton;
	
	private String SaveSelectionButton;

	private boolean authenticated;

	public List<JSDataSet> getDataSets()
	{
		return dataSets != null ? dataSets : Collections.<JSDataSet> emptyList();
	}

	public void setDataSets(List<JSDataSet> dataSets)
	{
		this.dataSets = dataSets;
	}

	
	public String getShowViewButton() {
		return 	this.ShowViewButton;
	}
	
	public void setShowViewButton(String ShowViewButton) {
		this.ShowViewButton = ShowViewButton;
	}
	
	public String getSaveSelectionButton() {
		return 	this.SaveSelectionButton;
	}
	
	public void setSaveSelectionButton(String SaveSelectionButton) {
		this.SaveSelectionButton = SaveSelectionButton;
	}


	public boolean isAuthenticated()
	{
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated)
	{
		this.authenticated = authenticated;
	}

}
