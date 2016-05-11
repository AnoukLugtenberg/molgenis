package org.molgenis.data.elasticsearch.reindex.job;

import com.google.common.collect.Lists;
import org.mockito.Mock;
import org.molgenis.data.*;
import org.molgenis.data.elasticsearch.ElasticsearchService.IndexingMode;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.elasticsearch.reindex.ReindexActionRegisterService;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionJobMetaData;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.CudType;
import org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.DataType;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.support.DefaultEntity;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.security.core.Authentication;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.REINDEX_STATUS;
import static org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.ReindexStatus.FAILED;
import static org.molgenis.data.elasticsearch.reindex.meta.ReindexActionMetaData.ReindexStatus.FINISHED;
import static org.molgenis.data.elasticsearch.reindex.meta.ReindexActionRegisterConfig.BACKEND;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class ReindexJobTest
{
	@Mock
	private Progress progress;
	@Mock
	private Authentication authentication;
	@Mock
	private DataService dataService;
	@Mock
	private SearchService searchService;

	private final String transactionId = "aabbcc";

	private ReindexActionJobMetaData reindexActionJobMetaData = new ReindexActionJobMetaData(BACKEND);
	private ReindexActionMetaData reindexActionMetaData = new ReindexActionMetaData(reindexActionJobMetaData, BACKEND);
	private ReindexActionRegisterService reindexActionRegisterService;
	private ReindexJob reindexJob;
	private Entity reindexActionJob;

	@BeforeMethod
	public void beforeMethod()
	{
		initMocks(this);
		reindexActionRegisterService = new ReindexActionRegisterService(dataService, reindexActionJobMetaData,
				reindexActionMetaData);
		reindexJob = new ReindexJob(progress, authentication, transactionId, dataService, searchService);
		reindexActionJob = reindexActionRegisterService.createReindexActionJob(transactionId);
		when(dataService.findOneById(ReindexActionJobMetaData.ENTITY_NAME, transactionId)).thenReturn(reindexActionJob);
	}

	@Test
	public void testNoReindexActionJobForTransaction()
	{
		when(dataService.findOneById(ReindexActionJobMetaData.ENTITY_NAME, this.transactionId)).thenReturn(null);
		mockGetAllReindexActions(this.transactionId, Stream.empty());

		reindexJob.call(this.progress);

		verify(progress).status("No reindex actions found for transaction id: [aabbcc]");
		verify(searchService, never()).refreshIndex();
	}

	@Test
	public void testNoReindexActionsForTransaction()
	{
		mockGetAllReindexActions(this.transactionId, Lists.<Entity>newArrayList().stream());

		reindexJob.call(this.progress);

		verify(progress).status("No reindex actions found for transaction id: [aabbcc]");
		verify(searchService, never()).refreshIndex();
	}

	private void mockGetAllReindexActions(String transactionId, Stream<Entity> entities)
	{
		Query<Entity> q = ReindexJob.createQueryGetAllReindexActions(transactionId);
		when(dataService.findAll(ReindexActionMetaData.ENTITY_NAME, q)).thenReturn(entities);
	}

	@Test
	public void testCreateQueryGetAllReindexActions()
	{
		Query<Entity> q = ReindexJob.createQueryGetAllReindexActions("testme");
		assertEquals(q.toString(),
				"rules=['reindexActionGroup' = 'testme'], sort=Sort [orders=[Order [attr=actionOrder, direction=ASC]]]");
	}

	@Test
	public void rebuildIndexDeleteSingleEntityTest()
	{
		Entity reindexAction = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", CudType.DELETE, DataType.DATA, "entityId",
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));
		mockGetAllReindexActions(this.transactionId, Stream.of(reindexAction));

		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		Entity toReindexEntity = new DefaultEntity(emd, dataService);
		when(dataService.findOneById("test", "entityId")).thenReturn(toReindexEntity);

		reindexJob.call(this.progress);
		assertEquals(reindexAction.get(REINDEX_STATUS), FINISHED.name());

		verify(this.searchService).deleteById("entityId", emd);
		verify(this.progress).progress(0, "Reindexing test.entityId, CUDType = " + CudType.DELETE);
		verify(this.progress).progress(1, "refreshIndex done.");
		verify(dataService, times(2)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction);
	}

	@Test
	public void rebuildIndexCreateSingleEntityTest()
	{
		this.rebuildIndexSingleEntityTest(CudType.CREATE, IndexingMode.ADD);
	}

	@Test
	public void rebuildIndexUpdateSingleEntityTest()
	{
		this.rebuildIndexSingleEntityTest(CudType.UPDATE, IndexingMode.UPDATE);
	}

	private void rebuildIndexSingleEntityTest(CudType cudType, IndexingMode indexingMode)
	{
		Entity reindexAction = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", cudType, DataType.DATA, "entityId",
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));
		mockGetAllReindexActions(this.transactionId, Stream.of(reindexAction));

		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		Entity toReindexEntity = new DefaultEntity(emd, dataService);
		when(dataService.findOneById("test", "entityId")).thenReturn(toReindexEntity);

		reindexJob.call(this.progress);
		assertEquals(reindexAction.get(REINDEX_STATUS), FINISHED.name());

		verify(this.searchService).index(toReindexEntity, emd, indexingMode);
		verify(this.progress).progress(0, "Reindexing test.entityId, CUDType = " + cudType.name());
		verify(this.progress).progress(1, "refreshIndex done.");
		verify(dataService, times(2)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction);
	}

	@Test
	public void rebuildIndexCreateBatchEntitiesTest()
	{
		this.rebuildIndexBatchEntitiesTest(CudType.CREATE);
	}

	@Test
	public void rebuildIndexDeleteBatchEntitiesTest()
	{
		this.rebuildIndexBatchEntitiesTest(CudType.DELETE);
	}

	@Test
	public void rebuildIndexUpdateBatchEntitiesTest()
	{
		this.rebuildIndexBatchEntitiesTest(CudType.UPDATE);
	}

	private void rebuildIndexBatchEntitiesTest(CudType cudType)
	{
		Entity reindexAction = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", cudType, DataType.DATA, null,
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));
		mockGetAllReindexActions(this.transactionId, Stream.of(reindexAction));

		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		reindexJob.call(this.progress);
		assertEquals(reindexAction.get(REINDEX_STATUS), FINISHED.name());

		verify(this.searchService)
				.rebuildIndex(this.dataService.getRepository("any"), new DefaultEntityMetaData("test"));
		verify(this.progress).progress(0, "Reindexing repository test. CUDType = " + cudType.name());
		verify(this.progress).progress(1, "refreshIndex done.");
		verify(dataService, times(2)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction);
	}

	@Test
	public void rebuildIndexCreateMetaDataTest()
	{
		this.rebuildIndexMetaDataTest(CudType.CREATE);
	}

	@Test
	public void rebuildIndexUpdateMetaDataTest()
	{
		this.rebuildIndexMetaDataTest(CudType.UPDATE);
	}

	private void rebuildIndexMetaDataTest(CudType cudType)
	{
		Entity reindexAction = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", cudType, DataType.METADATA, null,
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));
		mockGetAllReindexActions(this.transactionId, Stream.of(reindexAction));

		//TODO: move to beforeMethod block
		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		reindexJob.call(this.progress);
		assertEquals(reindexAction.get(REINDEX_STATUS), FINISHED.name());

		verify(this.searchService)
				.rebuildIndex(this.dataService.getRepository("any"), new DefaultEntityMetaData("test"));
		verify(this.progress).progress(0, "Reindexing repository test. CUDType = " + cudType.name());
		verify(this.progress).progress(1, "refreshIndex done.");
		verify(dataService, times(2)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction);
	}

	@Test
	public void rebuildIndexDeleteMetaDataEntityTest()
	{
		Entity reindexAction = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", CudType.DELETE, DataType.METADATA, null,
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));
		mockGetAllReindexActions(this.transactionId, Stream.of(reindexAction));

		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		reindexJob.call(this.progress);
		assertEquals(reindexAction.get(REINDEX_STATUS), FINISHED.name());

		verify(this.searchService).delete("test");
		verify(this.progress).progress(0, "Dropping index of repository test.");
		verify(this.progress).progress(1, "refreshIndex done.");
		verify(dataService, times(2)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction);
	}

	@Test
	public void reindexSingleEntitySearchServiceThrowsExceptionOnSecondEntityId()
	{
		Entity reindexAction1 = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", CudType.DELETE, DataType.DATA, "entityId1",
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));

		Entity reindexAction2 = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", CudType.DELETE, DataType.DATA, "entityId2",
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));

		Entity reindexAction3 = reindexActionRegisterService
				.createReindexAction(reindexActionJob, "test", CudType.DELETE, DataType.DATA, "entityId3",
						reindexActionRegisterService.increaseCountReindexActionJob(reindexActionJob));

		mockGetAllReindexActions(this.transactionId, Stream.of(reindexAction1, reindexAction2, reindexAction3));

		//TODO: move to beforeMethod block
		MetaDataService mds = mock(MetaDataService.class);
		when(dataService.getMeta()).thenReturn(mds);
		EntityMetaData emd = new DefaultEntityMetaData("test");
		when(mds.getEntityMetaData("test")).thenReturn(emd);

		MolgenisDataException mde = new MolgenisDataException("Random unrecoverable exception");
		doThrow(mde).when(searchService).deleteById("entityId2", emd);

		try
		{
			reindexJob.call(progress);
		}
		catch (Exception expected)
		{
			assertSame(expected, mde);
		}

		verify(searchService).deleteById("entityId1", emd);
		verify(searchService).deleteById("entityId2", emd);
		verify(searchService).deleteById("entityId3", emd);

		verify(searchService).refreshIndex();

		// Make sure the action status got updated and that the actionJob didn't get deleted
		assertEquals(reindexAction1.get(REINDEX_STATUS), FINISHED.name());
		assertEquals(reindexAction2.get(REINDEX_STATUS), FAILED.name());
		verify(dataService, atLeast(1)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction1);
		verify(dataService, atLeast(1)).update(ReindexActionMetaData.ENTITY_NAME, reindexAction2);
		verify(dataService, never()).delete(ReindexActionJobMetaData.ENTITY_NAME, reindexActionJob);
	}
}
