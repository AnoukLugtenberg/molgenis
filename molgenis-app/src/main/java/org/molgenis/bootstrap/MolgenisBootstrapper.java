package org.molgenis.bootstrap;

import static java.util.Objects.requireNonNull;

import org.molgenis.data.idcard.IdCardBootstrapper;
import org.molgenis.data.jobs.JobBootstrapper;
import org.molgenis.data.meta.system.SystemEntityMetaDataBootstrapper;
import org.molgenis.file.ingest.meta.FileIngesterJobRegistrar;
import org.molgenis.security.core.runas.RunAsSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application bootstrapper
 */
@Component
class MolgenisBootstrapper implements ApplicationListener<ContextRefreshedEvent>, PriorityOrdered
{
	private static final Logger LOG = LoggerFactory.getLogger(MolgenisBootstrapper.class);

	private final RegistryBootstrapper registryBootstrapper;
	private final SystemEntityMetaDataBootstrapper systemEntityMetaDataBootstrapper;
	private final RepositoryPopulator repositoryPopulator;
	private final FileIngesterJobRegistrar fileIngesterJobRegistrar;
	private final JobBootstrapper jobBootstrapper;
	private final IdCardBootstrapper idCardBootstrapper;

	@Autowired
	public MolgenisBootstrapper(RegistryBootstrapper registryBootstrapper,
			SystemEntityMetaDataBootstrapper systemEntityMetaDataBootstrapper,
			RepositoryPopulator repositoryPopulator, FileIngesterJobRegistrar fileIngesterJobRegistrar,
			JobBootstrapper jobBootstrapper, IdCardBootstrapper idCardBootstrapper)
	{
		this.registryBootstrapper = requireNonNull(registryBootstrapper);
		this.systemEntityMetaDataBootstrapper = requireNonNull(systemEntityMetaDataBootstrapper);
		this.repositoryPopulator = requireNonNull(repositoryPopulator);
		this.fileIngesterJobRegistrar = requireNonNull(fileIngesterJobRegistrar);
		this.jobBootstrapper = requireNonNull(jobBootstrapper);
		this.idCardBootstrapper = requireNonNull(idCardBootstrapper);
	}

	@Transactional
	@RunAsSystem
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		// TODO migration
		// TODO index rebuilding

		LOG.info("Bootstrapping application ...");

		LOG.trace("Bootstrapping registries ...");
		registryBootstrapper.bootstrap(event);
		LOG.debug("Bootstrapped registries");

		LOG.trace("Bootstrapping system entity meta data ...");
		systemEntityMetaDataBootstrapper.bootstrap(event);
		LOG.debug("Bootstrapped system entity meta data");

		LOG.trace("Populating repositories ...");
		repositoryPopulator.populate(event);
		LOG.debug("Populated repositories");

		LOG.trace("Bootstrapping jobs ...");
		jobBootstrapper.bootstrap();
		LOG.debug("Bootstrapped jobs");

		LOG.trace("Scheduling file ingest jobs ...");
		fileIngesterJobRegistrar.scheduleJobs();
		LOG.debug("Scheduled file ingest jobs");

		LOG.trace("Bootstrapping ID Card scheduler ...");
		idCardBootstrapper.bootstrap();
		LOG.debug("Bootstrapped ID Card scheduler");

		LOG.info("Bootstrapping application completed");
	}

	@Override
	public int getOrder()
	{
		return PriorityOrdered.HIGHEST_PRECEDENCE; // bootstrap application before doing anything else
	}
}
