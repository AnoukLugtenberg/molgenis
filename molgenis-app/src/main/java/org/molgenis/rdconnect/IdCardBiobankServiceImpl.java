package org.molgenis.rdconnect;

import static java.util.Objects.requireNonNull;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.settings.SettingsEntityListener;
import org.molgenis.rdconnect.IdCardBiobankRepository.IndexAction;
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.quartz.CronScheduleBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
public class IdCardBiobankServiceImpl implements IdCardBiobankService, ApplicationListener<ContextRefreshedEvent>
{
	private static final Logger LOG = LoggerFactory.getLogger(IdCardBiobankServiceImpl.class);

	private static final String ID_CARD_INDEX_REBUILD_TRIGGER_KEY = "idCardIndexRebuildTrigger";
	private static final String ID_CARD_INDEX_REBUILD_JOB_KEY = "idCardIndexRebuildJob";

	private final IdCardBiobankRepository idCardBiobankRepository;
	private final IdCardBiobankIndexerSettings idCardBiobankIndexerSettings;
	private final Scheduler scheduler;
	private final DataService dataService;

	@Autowired
	public IdCardBiobankServiceImpl(IdCardBiobankRepository idCardBiobankRepository,
			IdCardBiobankIndexerSettings idCardBiobankIndexerSettings, Scheduler scheduler, DataService dataService)
	{
		this.idCardBiobankRepository = requireNonNull(idCardBiobankRepository);
		this.idCardBiobankIndexerSettings = requireNonNull(idCardBiobankIndexerSettings);
		this.scheduler = requireNonNull(scheduler);
		this.dataService = requireNonNull(dataService);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		idCardBiobankIndexerSettings.addListener(new SettingsEntityListener()
		{
			@Override
			public void postUpdate(Entity entity)
			{
				try
				{
					updateIndexerScheduler(false);
				}
				catch (SchedulerException e)
				{
					throw new RuntimeException(e);
				}
			}
		});

		try
		{
			updateIndexerScheduler(true);
		}
		catch (SchedulerException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void updateIndexerScheduler(boolean initScheduler) throws SchedulerException
	{
		JobKey jobKey = new JobKey(ID_CARD_INDEX_REBUILD_JOB_KEY);
		if (idCardBiobankIndexerSettings.getBiobankIndexingEnabled())
		{
			String biobankIndexingFrequency = idCardBiobankIndexerSettings.getBiobankIndexingFrequency();
			Trigger trigger = createCronTrigger(biobankIndexingFrequency);
			if (!scheduler.checkExists(jobKey))
			{
				LOG.info("Scheduling index rebuild job with cron [{}]", biobankIndexingFrequency);
				JobDetail job = JobBuilder.newJob(ReindexJob.class).withIdentity(ID_CARD_INDEX_REBUILD_JOB_KEY).build();
				scheduler.scheduleJob(job, trigger);

				if (!initScheduler)
				{
					// write log event to db
					IdCardIndexingEvent idCardIndexingEvent = new IdCardIndexingEvent(dataService);
					idCardIndexingEvent.setStatus(IdCardIndexingEventStatus.CONFIGURATION_CHANGE);
					idCardIndexingEvent.setMessage("Indexing enabled");
					RunAsSystemProxy.runAsSystem(() -> {
						dataService.add(IdCardIndexingEvent.ENTITY_NAME, idCardIndexingEvent);
					});
				}
			}
			else
			{
				TriggerKey triggerKey = new TriggerKey(ID_CARD_INDEX_REBUILD_TRIGGER_KEY);
				Trigger oldTrigger = scheduler.getTrigger(triggerKey);

				// trigger.equals(oldTrigger) doesn't return true when the cron expressions are equal
				if (trigger instanceof CronTriggerImpl && oldTrigger instanceof CronTriggerImpl
						&& !((CronTriggerImpl) trigger).getCronExpression()
								.equals(((CronTriggerImpl) oldTrigger).getCronExpression()))
				{
					LOG.info("Rescheduling index rebuild job with cron [{}]", biobankIndexingFrequency);
					scheduler.rescheduleJob(triggerKey, trigger);

					if (!initScheduler)
					{
						// write log event to db
						IdCardIndexingEvent idCardIndexingEvent = new IdCardIndexingEvent(dataService);
						idCardIndexingEvent.setStatus(IdCardIndexingEventStatus.CONFIGURATION_CHANGE);
						idCardIndexingEvent
								.setMessage(String.format("Indexing schedule update [%s]", biobankIndexingFrequency));
						RunAsSystemProxy.runAsSystem(() -> {
							dataService.add(IdCardIndexingEvent.ENTITY_NAME, idCardIndexingEvent);
						});
					}
				}
			}
		}
		else
		{
			if (scheduler.checkExists(jobKey))
			{
				LOG.info("Deleting index rebuild job");
				scheduler.deleteJob(jobKey);

				if (!initScheduler)
				{
					// write log event to db
					IdCardIndexingEvent idCardIndexingEvent = new IdCardIndexingEvent(dataService);
					idCardIndexingEvent.setStatus(IdCardIndexingEventStatus.CONFIGURATION_CHANGE);
					idCardIndexingEvent.setMessage("Indexing disabled");
					RunAsSystemProxy.runAsSystem(() -> {
						dataService.add(IdCardIndexingEvent.ENTITY_NAME, idCardIndexingEvent);
					});
				}
			}
		}
	}

	@Override
	public void rebuildIndex()
	{
		idCardBiobankRepository.rebuildIndex(IndexAction.MANUAL);
	}

	@DisallowConcurrentExecution
	public static class ReindexJob implements Job
	{
		@Autowired
		private IdCardBiobankRepository idCardBiobankRepository;

		@Override
		public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
		{
			LOG.info("Executing scheduled rebuild index job ...");
			idCardBiobankRepository.rebuildIndex(IndexAction.SCHEDULED);
			LOG.debug("Executed scheduled rebuild index job");
		}
	}

	private Trigger createCronTrigger(String cronExpression)
	{
		return TriggerBuilder.newTrigger().withIdentity(ID_CARD_INDEX_REBUILD_TRIGGER_KEY)
				.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
	}
}
