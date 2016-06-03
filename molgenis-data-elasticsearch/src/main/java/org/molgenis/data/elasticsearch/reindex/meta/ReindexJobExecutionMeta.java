package org.molgenis.data.elasticsearch.reindex.meta;

import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

import static org.molgenis.data.jobs.JobExecutionMetaData.JOB_EXECUTION_META_DATA;

/**
 * This entity is used to track the progress of the execution of a ReindexActionJob.
 */
@Component
public class ReindexJobExecutionMeta extends DefaultEntityMetaData
{
	/**
	 * Example: Transaction id can be used to group all actions into one transaction.
	 */
	public static final String ID = "id";
	public static final String REINDEX_ACTION_JOB_ID = "reindexActionJobID";
	public static final String REINDEX_JOB_EXECUTION = "ReindexJobExecution";

	public ReindexJobExecutionMeta()
	{
		super(REINDEX_JOB_EXECUTION);
		setExtends(JOB_EXECUTION_META_DATA);
		addAttribute(REINDEX_ACTION_JOB_ID)
				.setDescription(
						"ID of the ReindexActionJob that contains the group of ReindexActions that this reindex job execution will reindex.")
				.setNillable(false);
	}
}