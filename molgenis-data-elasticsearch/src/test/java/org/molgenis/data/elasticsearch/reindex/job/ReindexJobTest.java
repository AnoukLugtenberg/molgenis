package org.molgenis.data.elasticsearch.reindex.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.stream.Stream;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Query;
import org.molgenis.data.elasticsearch.ElasticsearchService.IndexingMode;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.elasticsearch.reindex.ReindexActionRegisterService;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionJobMetaData;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.CudType;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.DataType;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.ReindexStatus;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionRegisterConfig;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.support.DefaultEntity;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

public class ReindexJobTest
{
	private static final Logger LOG = LoggerFactory.getLogger(ReindexJobTest.class);
	private Progress progress;
	private Authentication authentication;
	private String transactionId;
	private DataService dataService;
	private SearchService searchService;
	private ReindexActionJobMetaData reindexActionJobMetaData = new ReindexActionJobMetaData(
			ReindexActionRegisterConfig.BACKEND);
	private ReindexActionMetaData reindexActionMetaData = new ReindexActionMetaData(reindexActionJobMetaData,
			ReindexActionRegisterConfig.BACKEND);
	private ReindexActionRegisterService reindexActionRegisterService;
	private ReindexJobFactory reindexJobFactory;
	private RebuildIndexServiceImpl rebuildIndexServiceImpl;

	@BeforeMethod
	public void beforeMethod()
	{
		this.progress = mock(Progress.class);
		this.authentication = mock(Authentication.class);
		this.transactionId = "aabbcc";
		this.dataService = mock(DataService.class);
		this.searchService = mock(SearchService.class);
		this.reindexJobFactory = new ReindexJobFactory(this.dataService, this.searchService);
		this.rebuildIndexServiceImpl = new RebuildIndexServiceImpl(dataService, reindexJobFactory);
		this.reindexActionRegisterService = new ReindexActionRegisterService(dataService, reindexActionJobMetaData,
				reindexActionMetaData);
	}

	@Test
	public void call()
	{
		ReindexJob reindexJob = new ReindexJob(this.progress, this.authentication, this.transactionId,
				this.dataService, this.searchService);
		Entity reindexActionJob = reindexActionRegisterService.createReindexActionJob(this.transactionId);
		when(this.dataService.findOneById(ReindexActionJobMetaData.ENTITY_NAME, this.transactionId)).thenReturn(
				reindexActionJob);
		mockGetAllReindexActions(reindexJob, this.transactionId, Lists.<Entity> newArrayList().stream());
		reindexJob.call(this.progress);
	}

	private void mockGetAllReindexActions(ReindexJob reindexJob, String transactionId, Stream<Entity> entities)
	{
		Query<Entity> q = ReindexJob.createQueryGetAllReindexActions(transactionId);
		when(dataService.findAll(ReindexActionMetaData.ENTITY_NAME, q)).thenReturn(entities);
	}

	@Test
	private void testCreateQueryGetAllReindexActions()
	{
		Query<Entity> q = ReindexJob.createQueryGetAllReindexActions("testme");
		assertEquals(q.toString(),
				"rules=['reindexActionGroup' = 'testme'], sort=Sort [orders=[Order [attr=actionOrder, direction=ASC]]]");
	}

	@Test
	private void rebuildIndexCreateSingleEntity()
	{
		ReindexJob reindexJob = new ReindexJob(this.progress, this.authentication, this.transactionId,
				this.dataService, this.searchService);

		Entity reindexActionJob = reindexActionRegisterService.createReindexActionJob(this.transactionId);
		when(this.dataService.findOneById(ReindexActionJobMetaData.ENTITY_NAME, this.transactionId)).thenReturn(
				reindexActionJob);

		Entity entity = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", CudType.CREATE, DataType.DATA, "entityId",
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));
		mockGetAllReindexActions(reindexJob, this.transactionId, Lists.<Entity> newArrayList(entity).stream());

		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		Entity toReindexEntity = new DefaultEntity(emd, dataService);
		when(dataService.findOneById("test", "entityId")).thenReturn(toReindexEntity);

		reindexJob.call(this.progress);
		assertEquals(entity.get(ReindexActionMetaData.REINDEX_STATUS), ReindexStatus.FINISHED.name());
		
		verify(this.searchService).index(toReindexEntity, emd, IndexingMode.ADD);
		verify(this.progress).progress(0, "Reindexing test.entityId, CUDType = CREATE");
		verify(this.progress).progress(1, "refreshIndex done.");
		verify(dataService, times(2)).update(ReindexActionMetaData.ENTITY_NAME, entity);
	}
}
