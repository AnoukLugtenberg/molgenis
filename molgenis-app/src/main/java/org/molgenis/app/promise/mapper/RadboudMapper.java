package org.molgenis.app.promise.mapper;

import org.molgenis.app.promise.client.PromiseDataParser;
import org.molgenis.app.promise.mapper.MappingReport.Status;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.app.promise.model.BbmriNlCheatSheet.SAMPLE_COLLECTIONS_ENTITY;
import static org.molgenis.app.promise.model.PromiseMappingProjectMetaData.CREDENTIALS;

@Component
class RadboudMapper implements PromiseMapper, ApplicationListener<ContextRefreshedEvent>
{
	private static final Logger LOG = LoggerFactory.getLogger(RadboudMapper.class);

	static final String XML_ID = "ID";
	static final String XML_IDAA = "IDAA";

	private final String MAPPER_ID = "RADBOUD";

	private final PromiseMapperFactory promiseMapperFactory;
	private final PromiseDataParser promiseDataParser;

	private final DataService dataService;

	@Autowired
	public RadboudMapper(PromiseDataParser promiseDataParser, DataService dataService,
			PromiseMapperFactory promiseMapperFactory)
	{
		this.promiseDataParser = requireNonNull(promiseDataParser);
		this.dataService = requireNonNull(dataService);
		this.promiseMapperFactory = requireNonNull(promiseMapperFactory);
	}

	@Override
	public String getId()
	{
		return MAPPER_ID;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0)
	{
		promiseMapperFactory.registerMapper(MAPPER_ID, this);
	}

	@Override
	public MappingReport map(Entity project)
	{
		requireNonNull(project);

		MappingReport report = new MappingReport();
		try
		{
			RadboudSampleMap samples = new RadboudSampleMap(dataService);
			RadboudDiseaseMap diseases = new RadboudDiseaseMap(dataService);
			RadboudBiobankMapper biobankMapper = new RadboudBiobankMapper(dataService);

			Entity credentials = project.getEntity(CREDENTIALS);

			LOG.info("Generating RADBOUD sample");
			promiseDataParser.parse(credentials, 1, samples::addSample);

			LOG.info("Generating RADBOUD disease");
			promiseDataParser.parse(credentials, 2, diseases::addDisease);

			promiseDataParser.parse(credentials, 0, biobankEntity -> {
				String biobankId = biobankEntity.getString(XML_ID) + biobankEntity.getString(XML_IDAA);
				Entity bbmriSampleCollection = dataService.findOne(SAMPLE_COLLECTIONS_ENTITY, biobankId);
				if (bbmriSampleCollection == null)
				{
					bbmriSampleCollection = biobankMapper.mapNewBiobank(biobankEntity, samples, diseases);
					dataService.add(SAMPLE_COLLECTIONS_ENTITY, bbmriSampleCollection);
				}
				else
				{
					bbmriSampleCollection = biobankMapper.mapExistingBiobank(biobankEntity, samples, diseases, bbmriSampleCollection);
					dataService.update(SAMPLE_COLLECTIONS_ENTITY, bbmriSampleCollection);
				}
			});

			LOG.info("Finished mapping RADBOUD biobanks");
			report.setStatus(Status.SUCCESS);
		}
		catch (Exception e)
		{
			report.setStatus(Status.ERROR);
			report.setMessage(e.getMessage());

			LOG.warn("Error in mapping response to entities", e);
		}

		return report;
	}

	static String getBiobankId(Entity radboudEntity)
	{
		return radboudEntity.getString(XML_ID) + "_" + radboudEntity.getString(XML_IDAA);
	}
}
