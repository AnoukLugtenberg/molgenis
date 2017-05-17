package org.molgenis.file.ingest.execution;

import org.molgenis.data.jobs.Job;
import org.molgenis.data.jobs.JobFactory;
import org.molgenis.data.jobs.model.ScheduledJobType;
import org.molgenis.data.jobs.model.ScheduledJobTypeFactory;
import org.molgenis.file.ingest.meta.FileIngestJobExecution;
import org.molgenis.file.ingest.meta.FileIngestJobExecutionMetaData;
import org.molgenis.ui.menu.MenuReaderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import static java.text.MessageFormat.format;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("SpringJavaAutowiringInspection")
@Configuration
@Import(FileIngester.class)
public class FileIngestConfig
{
	private final FileIngester fileIngester;
	private final ScheduledJobTypeFactory scheduledJobTypeFactory;
	private final FileIngestJobExecutionMetaData fileIngestJobExecutionMetaData;
	private final MenuReaderService menuReaderService;

	public FileIngestConfig(FileIngester fileIngester, ScheduledJobTypeFactory scheduledJobTypeFactory,
			FileIngestJobExecutionMetaData fileIngestJobExecutionMetaData, MenuReaderService menuReaderService)
	{
		this.fileIngester = requireNonNull(fileIngester);
		this.scheduledJobTypeFactory = requireNonNull(scheduledJobTypeFactory);
		this.fileIngestJobExecutionMetaData = requireNonNull(fileIngestJobExecutionMetaData);
		this.menuReaderService = requireNonNull(menuReaderService);
	}

	/**
	 * The FileIngestJob Factory bean.
	 */
	@Bean
	public JobFactory<FileIngestJobExecution> fileIngestJobFactory()
	{
		return new JobFactory<FileIngestJobExecution>()
		{
			@Override
			public Job createJob(FileIngestJobExecution fileIngestJobExecution)
			{
				final String targetEntityId = fileIngestJobExecution.getTargetEntityId();
				final String url = fileIngestJobExecution.getUrl();
				final String loader = fileIngestJobExecution.getLoader();
				String dataExplorerURL = menuReaderService.getMenu().findMenuItemPath("dataexplorer");
				fileIngestJobExecution.setResultUrl(format("{0}?entity={1}", dataExplorerURL, targetEntityId));
				return progress -> fileIngester
						.ingest(targetEntityId, url, loader, fileIngestJobExecution.getIdentifier(), progress);
			}
		};
	}

	@Lazy
	@Bean
	public ScheduledJobType fileIngestJobType()
	{
		ScheduledJobType result = scheduledJobTypeFactory.create("fileIngest");
		result.setLabel("File ingest");
		result.setDescription("This job downloads a file from a URL and imports it into MOLGENIS.");
		result.setSchema("{\"title\": \"FileIngest Job\",\n \"type\": \"object\",\n \"properties\": {\n"
				+ "\"url\": {\n\"type\": \"string\",\n\"format\": \"uri\",\n"
				+ "\"description\": \"URL to download the file to ingest from\"\n    },\n"
				+ "\"loader\": {\n \"enum\": [ \"CSV\" ]\n },\n \"targetEntityId\": {\n"
				+ "\"type\": \"string\",\n \"description\": \"ID of the entity to import to\"\n"
				+ "}\n  },\n  \"required\": [\n \"url\",\n \"loader\",\n \"targetEntityId\"\n]\n}");
		result.setJobExecutionType(fileIngestJobExecutionMetaData);
		return result;
	}
}
