package org.molgenis.omx.search;

import org.molgenis.security.runas.RunAsSystem;

import java.util.List;

/**
 * Indexes DataSet matrices.
 * 
 * @author erwin
 * 
 */
public interface DataSetsIndexer
{
	void index();

	void indexDataSets(List<Integer> datasetIds);

	void indexProtocols(List<Integer> protocolIds);

	/**
	 * Check if there is an indexing job running
	 * 
	 * @return
	 */
	public boolean isIndexingRunning();

	@RunAsSystem
	void indexProtocolsSynced(List<Integer> protocolIds);
}
