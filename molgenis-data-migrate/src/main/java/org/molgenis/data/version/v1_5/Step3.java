package org.molgenis.data.version.v1_5;

import static org.molgenis.data.support.QueryImpl.EQ;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.meta.MetaDataServiceImpl;
import org.molgenis.data.meta.PackageMetaData;
import org.molgenis.data.meta.TagMetaData;
import org.molgenis.data.meta.migrate.v1_4.AttributeMetaDataMetaData1_4;
import org.molgenis.data.meta.migrate.v1_4.EntityMetaDataMetaData1_4;
import org.molgenis.data.mysql.AsyncJdbcTemplate;
import org.molgenis.data.mysql.MysqlRepository;
import org.molgenis.data.mysql.MysqlRepositoryCollection;
import org.molgenis.data.support.DataServiceImpl;
import org.molgenis.data.version.MetaDataUpgrade;
import org.molgenis.fieldtypes.MrefField;
import org.molgenis.security.runas.RunAsSystemProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;

/**
 * Migrates MySQL MREF tables for ordinary entities from molgenis 1.4 to 1.5
 */
public class Step3 extends MetaDataUpgrade
{
	private JdbcTemplate template;

	private DataServiceImpl dataService;

	private DataSource dataSource;

	private SearchService searchService;

	private static final Logger LOG = LoggerFactory.getLogger(Step3.class);

	private MetaDataService metaData;

	public Step3(DataSource dataSource, SearchService searchService)
	{
		super(2, 3);
		this.template = new JdbcTemplate(dataSource);
		this.dataSource = dataSource;
		this.searchService = searchService;
	}

	@Override
	public void upgrade()
	{
		LOG.info("Migrating MySQL MREF columns...");

		dataService = new DataServiceImpl();
		// Get the undecorated repos to index
		MysqlRepositoryCollection backend = new MysqlRepositoryCollection()
		{
			@Override
			protected MysqlRepository createMysqlRepository()
			{
				return new MysqlRepository(dataService, dataSource, new AsyncJdbcTemplate(new JdbcTemplate(dataSource)));
			}

			@Override
			public boolean hasRepository(String name)
			{
				throw new NotImplementedException("Not implemented yet");
			}
		};

		metaData = new MetaDataServiceImpl(dataService);
		RunAsSystemProxy.runAsSystem(() -> metaData.setDefaultBackend(backend));

		addOrderColumnToMREFTables(backend);
		updateAttributeOrderInMysql(dataService, searchService);
		recreateElasticSearchMetaDataIndices(searchService);
		LOG.info("Migrating MySQL MREF columns DONE.");
	}

	private void recreateElasticSearchMetaDataIndices(SearchService v14ElasticSearchService)
	{
		v14ElasticSearchService.delete("entities");
		v14ElasticSearchService.delete("attributes");
		v14ElasticSearchService.delete("tags");
		v14ElasticSearchService.delete("packages");

		searchService.refresh();

		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try
		{
			searchService.createMappings(new TagMetaData());
			searchService.createMappings(new PackageMetaData());
			searchService.createMappings(new AttributeMetaDataMetaData());
			searchService.createMappings(new EntityMetaDataMetaData());
		}
		catch (IOException e)
		{
			LOG.error("error creating metadata mappings", e);
		}

		// TODO: refill from mysql!
	}

	private void addOrderColumnToMREFTables(MysqlRepositoryCollection backend)
	{
		for (EntityMetaData emd : metaData.getEntityMetaDatas())
		{
			for (AttributeMetaData amd : emd.getAtomicAttributes())
			{
				if (amd.getDataType() instanceof MrefField)
				{
					LOG.info("Add order column to MREF attribute table {}.{} .", emd.getName(), amd.getName());
					String mrefUpdateSql = getMrefUpdateSql(emd, amd);
					LOG.info(mrefUpdateSql);
					try
					{
						template.execute(mrefUpdateSql);
					}
					catch (DataAccessException dae)
					{
						LOG.error("Error migrating {}.{} . {}", emd.getName(), amd.getName(), dae.getMessage());
					}
				}
			}
		}
	}

	private void updateAttributeOrderInMysql(DataServiceImpl v15MySQLDataService, SearchService v14ElasticSearchService)
	{
		LOG.info("Update attribute order in MySQL...");

		// save all entity metadata with attributes in proper order
		for (Entity v15EntityMetaDataEntity : v15MySQLDataService.getRepository(EntityMetaDataMetaData.ENTITY_NAME))
		{
			LOG.info("V1.5 Entity: " + v15EntityMetaDataEntity.get(EntityMetaDataMetaData1_4.SIMPLE_NAME));
			List<Entity> attributes = Lists.newArrayList(v14ElasticSearchService.search(
					EQ(AttributeMetaDataMetaData1_4.ENTITY,
							v15EntityMetaDataEntity.getString(EntityMetaDataMetaData1_4.SIMPLE_NAME)),
					new AttributeMetaDataMetaData1_4()));
			v15EntityMetaDataEntity.set(EntityMetaDataMetaData.ATTRIBUTES, attributes);
			v15EntityMetaDataEntity.set(EntityMetaDataMetaData.BACKEND, "MySQL");
			v15MySQLDataService.update(EntityMetaDataMetaData.ENTITY_NAME, v15EntityMetaDataEntity);
		}
		LOG.info("Update attribute order done.");
	}

	private static String getMrefUpdateSql(EntityMetaData emd, AttributeMetaData att)
	{
		return String.format("ALTER TABLE `%s_%s` ADD COLUMN `order` INT;", emd.getName().toLowerCase(), att.getName()
				.toLowerCase());
	}
}
