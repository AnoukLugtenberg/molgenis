package org.molgenis.integrationtest.platform;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.generate;
import static java.util.stream.Stream.of;
import static org.molgenis.data.RepositoryCapability.MANAGABLE;
import static org.molgenis.data.RepositoryCapability.QUERYABLE;
import static org.molgenis.data.RepositoryCapability.WRITABLE;
import static org.molgenis.data.Sort.Direction.DESC;
import static org.molgenis.integrationtest.data.harness.EntitiesHarness.ATTR_ID;
import static org.molgenis.integrationtest.data.harness.EntitiesHarness.ATTR_STRING;
import static org.molgenis.security.core.runas.RunAsSystemProxy.runAsSystem;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityListener;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCapability;
import org.molgenis.data.Sort;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.elasticsearch.reindex.job.ReindexService;
import org.molgenis.data.meta.MetaDataServiceImpl;
import org.molgenis.data.meta.model.EntityMetaData;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.integrationtest.data.harness.EntitiesHarness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.Iterators;

@ContextConfiguration(classes = { PlatformITConfig.class })
public class PlatformIT extends AbstractTestNGSpringContextTests
{
	private final Logger LOG = LoggerFactory.getLogger(PlatformIT.class);

	private static final String ENTITY_NAME = "test_TestEntity";
	private static final String REF_ENTITY_NAME = "test_TestRefEntity";
	private EntityMetaData entityMetaData;
	private EntityMetaData refEntityMetaData;

	@Autowired
	private ReindexService reindexService;
	@Autowired
	private EntitiesHarness testHarness;
	@Autowired
	private DataService dataService;
	@Autowired
	private SearchService searchService;
	@Autowired
	private MetaDataServiceImpl metaDataService;
	@Autowired
	private ConfigurableApplicationContext applicationContext;

	/**
	 * Wait till the whole index is stable. Reindex job is done a-synchronized.
	 */
	private void waitForWorkToBeFinished()
	{
		try
		{
			reindexService.waitForAllIndicesStable();
			LOG.info("<---- All work finished ---->");
		}
		catch (InterruptedException e)
		{
			LOG.warn("Interrupted while waiting for index to become stable!", e);
			fail("Interrupted while waiting for index to become stable!");
		}
	}

	/**
	 * Wait till the index is stable. Reindex job is done a-synchronized.
	 *
	 * @param entityName
	 */
	private void waitForIndexToBeStable(String entityName)
	{
		try
		{
			reindexService.waitForIndexToBeStableIncludingReferences(entityName);
			LOG.info("<---- index for entity [{}] incl. references is stable ---->", entityName);
		}
		catch (InterruptedException e)
		{
			LOG.info("Interrupted waiting for [{}] incl. references to become stable", entityName, e);
		}

	}

	@AfterClass
	public void cleanUp() throws InterruptedException
	{
		// Give asyncTransactionLog time to stop gracefully
		TimeUnit.SECONDS.sleep(1);

		applicationContext.close();
		SecurityContextHolder.getContext().setAuthentication(null);

		try
		{
			// Delete molgenis home folder
			FileUtils.deleteDirectory(new File(System.getProperty("molgenis.home")));
		}
		catch (IOException e)
		{
			LOG.error("Error removing molgenis home directory", e);
		}
	}

	@BeforeClass
	public void setUp()
	{
		Package p = null; // FIXME new PackageImpl("test");
		refEntityMetaData = testHarness.createRefEntityMetaData("TestRefEntity", p);
		entityMetaData = testHarness.createEntityMetaData("TestEntity", p, refEntityMetaData);

		metaDataService.addEntityMeta(refEntityMetaData);
		metaDataService.addEntityMeta(entityMetaData);
		this.waitForWorkToBeFinished();
		setAuthentication();
	}

	private void setAuthentication()
	{
		// Permissions ENTITY_NAME
		String writeTestEntity = "ROLE_ENTITY_WRITE_" + ENTITY_NAME.toUpperCase();
		String readTestEntity = "ROLE_ENTITY_READ_" + ENTITY_NAME.toUpperCase();
		String countTestEntity = "ROLE_ENTITY_COUNT_" + ENTITY_NAME.toUpperCase();

		// Permissions REF_ENTITY_NAME
		String readTestRefEntity = "ROLE_ENTITY_READ_" + REF_ENTITY_NAME.toUpperCase();
		String countTestRefEntity = "ROLE_ENTITY_COUNT_" + REF_ENTITY_NAME.toUpperCase();

		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("user", "user", writeTestEntity, readTestEntity, readTestRefEntity,
						countTestEntity, countTestRefEntity));
	}

	@AfterMethod
	public void afterMethod()
	{
		runAsSystem(() -> {
			dataService.deleteAll(ENTITY_NAME);
			dataService.deleteAll(REF_ENTITY_NAME);
		});
		waitForIndexToBeStable(ENTITY_NAME);
	}

	@Test
	public void testEntityListener()
	{
		List<Entity> refEntities = testHarness.createTestRefEntities(refEntityMetaData, 6);
		List<Entity> entities = testHarness.createTestEntities(entityMetaData, 2, 6);
		runAsSystem(() -> {
			dataService.add(REF_ENTITY_NAME, refEntities.stream());
			dataService.add(ENTITY_NAME, entities.stream());
		});
		waitForIndexToBeStable(ENTITY_NAME);

		AtomicInteger updateCalled = new AtomicInteger(0);
		EntityListener listener = new EntityListener()
		{
			@Override
			public Object getEntityId()
			{
				return entities.get(0).getIdValue();
			}

			@Override
			public void postUpdate(Entity entity)
			{
				updateCalled.incrementAndGet();
				assertEquals(entity.getIdValue(), entities.get(0).getIdValue());
			}
		};

		try
		{
			dataService.addEntityListener(ENTITY_NAME, listener);
			dataService.update(ENTITY_NAME, entities.stream());
			assertEquals(updateCalled.get(), 1);
			waitForIndexToBeStable(ENTITY_NAME);
			assertPresent(entities);
		}
		finally
		{
			dataService.removeEntityListener(ENTITY_NAME, listener);
			updateCalled.set(0);
			dataService.update(ENTITY_NAME, entities.stream());
			assertEquals(updateCalled.get(), 0);
			waitForIndexToBeStable(ENTITY_NAME);
			assertPresent(entities);
		}
	}

	@Test
	public void testAdd()
	{
		List<Entity> entities = create(2);
		assertEquals(searchService.count(entityMetaData), 0);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertEquals(dataService.count(ENTITY_NAME, new QueryImpl<>()), 2);
		assertEquals(searchService.count(entityMetaData), 2);
		assertPresent(entities);
	}

	@Test
	public void testCount()
	{
		List<Entity> entities = create(2);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertEquals(dataService.count(ENTITY_NAME, new QueryImpl<>()), 2);
		assertEquals(searchService.count(entityMetaData), 2);
		assertPresent(entities);
	}

	@Test
	public void testDelete()
	{
		Entity entity = create();
		dataService.add(ENTITY_NAME, entity);
		waitForIndexToBeStable(ENTITY_NAME);
		assertPresent(entity);

		dataService.delete(ENTITY_NAME, entity);
		waitForIndexToBeStable(ENTITY_NAME);
		assertNotPresent(entity);
	}

	@Test
	public void testDeleteById()
	{
		Entity entity = create();
		dataService.add(ENTITY_NAME, entity);
		waitForIndexToBeStable(ENTITY_NAME);
		assertPresent(entity);

		dataService.deleteById(ENTITY_NAME, entity.getIdValue());
		waitForIndexToBeStable(ENTITY_NAME);
		assertNotPresent(entity);
	}

	@Test
	public void testDeleteStream()
	{
		List<Entity> entities = create(2);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertEquals(dataService.count(ENTITY_NAME, new QueryImpl<>()), entities.size());

		dataService.delete(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertEquals(dataService.count(ENTITY_NAME, new QueryImpl<>()), 0);
	}

	@Test
	public void testDeleteAll()
	{
		List<Entity> entities = create(5);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertEquals(dataService.count(ENTITY_NAME, new QueryImpl<>()), entities.size());

		dataService.deleteAll(ENTITY_NAME);
		waitForIndexToBeStable(ENTITY_NAME);
		assertEquals(dataService.count(ENTITY_NAME, new QueryImpl<>()), 0);
	}

	@Test
	public void testFindAllEmpty()
	{
		Stream<Entity> retrieved = dataService.findAll(ENTITY_NAME);
		assertEquals(retrieved.count(), 0);
	}

	@Test
	public void testFindAll()
	{
		List<Entity> entities = create(5);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		Stream<Entity> retrieved = dataService.findAll(ENTITY_NAME);
		assertEquals(retrieved.count(), entities.size());
	}

	@Test
	public void testFindAllTyped()
	{
		// FIXME
		//		List<Entity> entities = create(1);
		//		dataService.add(ENTITY_NAME, entities.stream());
		//		waitForIndexToBeStable(ENTITY_NAME);
		//		Supplier<Stream<TestEntity>> retrieved = () -> dataService.findAll(ENTITY_NAME, TestEntity.class);
		//		assertEquals(retrieved.get().count(), 1);
		//		assertEquals(retrieved.get().iterator().next().getId(), entities.get(0).getIdValue());
	}

	@Test
	public void testFindAllByIds()
	{
		List<Entity> entities = create(5);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		Stream<Object> ids = Stream.concat(entities.stream().map(Entity::getIdValue), of("bogus"));
		Stream<Entity> retrieved = dataService.findAll(ENTITY_NAME, ids);
		assertEquals(retrieved.count(), entities.size());
	}

	@Test
	public void testFindAllByIdsTyped()
	{
		// FIXME
		//		List<Entity> entities = create(5);
		//		dataService.add(ENTITY_NAME, entities.stream());
		//		waitForIndexToBeStable(ENTITY_NAME);
		//
		//		Supplier<Stream<TestEntity>> retrieved = () -> dataService
		//				.findAll(ENTITY_NAME, Stream.concat(entities.stream().map(Entity::getIdValue), of("bogus")),
		//						TestEntity.class);
		//		assertEquals(retrieved.get().count(), entities.size());
		//		assertEquals(retrieved.get().iterator().next().getId(), entities.get(0).getIdValue());
	}

	@Test
	public void testFindAllStreamFetch()
	{
		List<Entity> entities = create(5);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		Stream<Object> ids = concat(entities.stream().map(Entity::getIdValue), of("bogus"));
		Stream<Entity> retrieved = dataService.findAll(ENTITY_NAME, ids, new Fetch().field(ATTR_ID));
		assertEquals(retrieved.count(), entities.size());
	}

	@Test
	public void testFindQuery()
	{
		List<Entity> entities = create(5);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		Supplier<Stream<Entity>> found = () -> dataService
				.findAll(ENTITY_NAME, new QueryImpl<>().eq(ATTR_ID, entities.get(0).getIdValue()));
		assertEquals(found.get().count(), 1);
		assertEquals(found.get().findFirst().get().getIdValue(), entities.get(0).getIdValue());
	}

	@Test
	public void testFindQueryLimit2_Offset2_sortOnInt()
	{
		List<Entity> testRefEntities = testHarness.createTestRefEntities(refEntityMetaData, 6);
		List<Entity> testEntities = testHarness.createTestEntities(entityMetaData, 10, 6);
		runAsSystem(() -> {
			dataService.add(REF_ENTITY_NAME, testRefEntities.stream());
			dataService.add(ENTITY_NAME, testEntities.stream());
		});
		waitForIndexToBeStable(REF_ENTITY_NAME);
		waitForIndexToBeStable(ENTITY_NAME);
		Supplier<Stream<Entity>> found = () -> dataService
				.findAll(ENTITY_NAME, new QueryImpl<>().pageSize(2).offset(2).sort(new Sort(ATTR_ID, DESC)));
		assertEquals(found.get().count(), 2);
		assertEquals(found.get().collect(toList()), Arrays.asList(testEntities.get(7), testEntities.get(6)));
	}

	@Test
	public void testFindQueryTyped()
	{
		// FIXME
		//		List<Entity> entities = create(5);
		//		dataService.add(ENTITY_NAME, entities.stream());
		//		waitForIndexToBeStable(ENTITY_NAME);
		//		Supplier<Stream<TestEntity>> found = () -> dataService
		//				.findAll(ENTITY_NAME, new QueryImpl<TestEntity>().eq(ATTR_ID, entities.get(0).getIdValue()),
		//						TestEntity.class);
		//		assertEquals(found.get().count(), 1);
		//		assertEquals(found.get().findFirst().get().getId(), entities.get(0).getIdValue());
	}

	@Test
	public void testFindOne()
	{
		List<Entity> entities = create(1);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertNotNull(dataService.findOneById(ENTITY_NAME, entities.get(0).getIdValue()));
	}

	@Test
	public void testFindOneTyped()
	{
		// FIXME
		//		List<Entity> entities = create(1);
		//		dataService.add(ENTITY_NAME, entities.stream());
		//		waitForIndexToBeStable(ENTITY_NAME);
		//		TestEntity testEntity = dataService.findOneById(ENTITY_NAME, entities.get(0).getIdValue(), TestEntity.class);
		//		assertNotNull(testEntity);
		//		assertEquals(testEntity.getId(), entities.get(0).getIdValue());
	}

	@Test
	public void testFindOneFetch()
	{
		List<Entity> entities = create(1);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		assertNotNull(dataService.findOneById(ENTITY_NAME, entities.get(0).getIdValue(), new Fetch().field(ATTR_ID)));
	}

	@Test
	public void testFindOneFetchTyped()
	{
		// FIXME
		//		List<Entity> entities = create(1);
		//		dataService.add(ENTITY_NAME, entities.stream());
		//		waitForIndexToBeStable(ENTITY_NAME);
		//		TestEntity testEntity = dataService
		//				.findOneById(ENTITY_NAME, entities.get(0).getIdValue(), new Fetch().field(ATTR_ID), TestEntity.class);
		//		assertNotNull(testEntity);
		//		assertEquals(testEntity.getId(), entities.get(0).getIdValue());
	}

	@Test
	public void testFindOneQuery()
	{
		List<Entity> entities = create(1);
		dataService.add(ENTITY_NAME, entities.stream());
		waitForIndexToBeStable(ENTITY_NAME);
		Entity entity = dataService.findOne(ENTITY_NAME, new QueryImpl<>().eq(ATTR_ID, entities.get(0).getIdValue()));
		assertNotNull(entity);
	}

	@Test
	public void testFindOneQueryTyped()
	{
		// FIXME
		//		List<Entity> entities = create(1);
		//		dataService.add(ENTITY_NAME, entities.stream());
		//		waitForIndexToBeStable(ENTITY_NAME);
		//		TestEntity entity = dataService
		//				.findOne(ENTITY_NAME, new QueryImpl<TestEntity>().eq(ATTR_ID, entities.get(0).getIdValue()),
		//						TestEntity.class);
		//		assertNotNull(entity);
		//		assertEquals(entity.getId(), entities.get(0).getIdValue());
	}

	@Test
	public void testGetCapabilities()
	{
		Set<RepositoryCapability> capabilities = dataService.getCapabilities(ENTITY_NAME);
		assertNotNull(capabilities);
		assertTrue(capabilities.containsAll(asList(MANAGABLE, QUERYABLE, WRITABLE)));
	}

	@Test
	public void testGetEntityMetaData()
	{
		EntityMetaData emd = dataService.getEntityMetaData(ENTITY_NAME);
		assertNotNull(emd);
		assertEquals(emd, entityMetaData);
	}

	@Test
	public void testGetEntityNames()
	{
		Stream<String> names = dataService.getEntityNames();
		assertNotNull(names);
		assertTrue(names.filter(ENTITY_NAME::equals).findFirst().isPresent());
	}

	@Test
	public void testGetMeta()
	{
		assertNotNull(dataService.getMeta());
	}

	@Test
	public void testGetRepository()
	{
		Repository<Entity> repo = dataService.getRepository(ENTITY_NAME);
		assertNotNull(repo);
		assertEquals(repo.getName(), ENTITY_NAME);

		try
		{
			dataService.getRepository("bogus");
			fail("Should have thrown UnknownEntityException");
		}
		catch (UnknownEntityException e)
		{
			// Expected
		}
	}

	@Test
	public void testHasRepository()
	{
		assertTrue(dataService.hasRepository(ENTITY_NAME));
		assertFalse(dataService.hasRepository("bogus"));
	}

	@Test
	public void testIterator()
	{
		assertNotNull(dataService.iterator());
		assertTrue(Iterators.contains(dataService.iterator(), dataService.getRepository(ENTITY_NAME)));
	}

	@Test
	public void testQuery()
	{
		assertNotNull(dataService.query(ENTITY_NAME));
		try
		{
			dataService.query("bogus");
			fail("Should have thrown UnknownEntityException");
		}
		catch (UnknownEntityException e)
		{
			// Expected
		}
	}

	@Test
	public void testUpdate()
	{
		Entity entity = create(1).get(0);
		dataService.add(ENTITY_NAME, entity);
		waitForIndexToBeStable(ENTITY_NAME);

		entity = dataService.findOneById(ENTITY_NAME, entity.getIdValue());
		assertNotNull(entity);
		assertNull(entity.get(ATTR_STRING));

		entity.set(ATTR_STRING, "qwerty");

		dataService.update(ENTITY_NAME, entity);
		waitForIndexToBeStable(ENTITY_NAME);
		assertPresent(entity);

		entity = dataService.findOneById(ENTITY_NAME, entity.getIdValue());
		assertNotNull(entity.get(ATTR_STRING));
		assertEquals(entity.get(ATTR_STRING), "qwerty");
	}

	@Test
	public void testUpdateStream()
	{
		Entity entity = create(1).get(0);

		dataService.add(ENTITY_NAME, entity);
		waitForIndexToBeStable(ENTITY_NAME);
		assertPresent(entity);

		entity = dataService.findOneById(ENTITY_NAME, entity.getIdValue());
		assertNotNull(entity);
		assertNull(entity.get(ATTR_STRING));

		entity.set(ATTR_STRING, "qwerty");

		dataService.update(ENTITY_NAME, of(entity));
		waitForIndexToBeStable(ENTITY_NAME);
		assertPresent(entity);

		entity = dataService.findOneById(ENTITY_NAME, entity.getIdValue());
		assertNotNull(entity.get(ATTR_STRING));
		assertEquals(entity.get(ATTR_STRING), "qwerty");
	}

	private List<Entity> create(int count)
	{
		return generate(this::create).limit(count).collect(toList());
	}

	private Entity create()
	{
		return null; // FIXME return new DefaultEntity(entityMetaData, dataService);
	}

	private void assertPresent(List<Entity> entities)
	{
		entities.forEach(this::assertPresent);
	}

	private void assertPresent(Entity entity)
	{
		// Found in PostgreSQL
		assertNotNull(dataService.findOneById(entityMetaData.getName(), entity.getIdValue()));

		// Found in index Elasticsearch
		Query<Entity> q = new QueryImpl<>();
		q.eq(entityMetaData.getIdAttribute().getName(), entity.getIdValue());
		assertEquals(searchService.count(q, entityMetaData), 1);
	}

	private void assertNotPresent(Entity entity)
	{
		// Found in PostgreSQL
		assertNull(dataService.findOneById(entityMetaData.getName(), entity.getIdValue()));

		// Not found in index Elasticsearch
		Query<Entity> q = new QueryImpl<>();
		q.eq(entityMetaData.getIdAttribute().getName(), entity.getIdValue());
		assertEquals(searchService.count(q, entityMetaData), 0);
	}
}
