package org.molgenis.data.elasticsearch.reindex.meta;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.Package.PACKAGE_SEPARATOR;
import static org.molgenis.data.reindex.meta.IndexPackage.PACKAGE_INDEX;

import org.molgenis.data.jobs.JobExecutionMetaData;
import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.molgenis.data.reindex.meta.IndexPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This entity is used to track the progress of the execution of a ReindexActionJob.
 */
@Component
public class ReindexJobExecutionMeta extends SystemEntityMetaDataImpl
{
	public static final String SIMPLE_NAME = "ReindexJobExecution";
	public static final String REINDEX_JOB_EXECUTION = PACKAGE_INDEX + PACKAGE_SEPARATOR + SIMPLE_NAME;

	/**
	 * Example: Transaction id can be used to group all actions into one transaction.
	 */
	public static final String ID = "id";
	public static final String REINDEX_ACTION_JOB_ID = "reindexActionJobID";

	private final IndexPackage indexPackage;
	private JobExecutionMetaData jobExecutionMetaData;

	@Autowired
	public ReindexJobExecutionMeta(IndexPackage indexPackage)
	{
		super(SIMPLE_NAME, PACKAGE_INDEX);
		this.indexPackage = requireNonNull(indexPackage);
	}

	@Override
	public void init()
	{
		setPackage(indexPackage);

		setExtends(jobExecutionMetaData);
		addAttribute(REINDEX_ACTION_JOB_ID).setDescription(
				"ID of the ReindexActionJob that contains the group of ReindexActions that this reindex job execution will reindex.")
				.setNillable(false);
	}

	// setter injection instead of constructor injection to avoid unresolvable circular dependencies
	@Autowired
	public void setJobExecutionMetaData(JobExecutionMetaData jobExecutionMetaData)
	{
		this.jobExecutionMetaData = requireNonNull(jobExecutionMetaData);
	}
}