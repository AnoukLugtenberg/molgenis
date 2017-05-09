package org.molgenis.data.jobs;

/**
 * Interface for molgenis jobs.
 */
public interface Job<Result>
{
	/**
	 * Execute this job.
	 *
	 * @param progress     the {@link Progress} to report progress to
	 * @throws Exception if something goes wrong
	 */
	Result call(Progress progress) throws Exception;
}
