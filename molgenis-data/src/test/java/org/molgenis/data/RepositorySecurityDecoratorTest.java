package org.molgenis.data;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mockito.ArgumentCaptor;
import org.molgenis.data.settings.AppSettings;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

public class RepositorySecurityDecoratorTest
{
	private String entityName;
	private EntityMetaData entityMeta;
	private Repository decoratedRepository;
	private AppSettings appSettings;
	private RepositorySecurityDecorator repositorySecurityDecorator;

	@BeforeMethod
	public void setUp()
	{
		entityName = "entity";
		entityMeta = mock(EntityMetaData.class);
		when(entityMeta.getName()).thenReturn(entityName);
		decoratedRepository = mock(Repository.class);
		when(decoratedRepository.getName()).thenReturn(entityName);
		when(decoratedRepository.getEntityMetaData()).thenReturn(entityMeta);
		appSettings = mock(AppSettings.class);
		repositorySecurityDecorator = new RepositorySecurityDecorator(decoratedRepository, appSettings);
	}

	@Test
	public void addStream()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_WRITE_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Stream<Entity> entities = Stream.empty();
		when(decoratedRepository.add(entities)).thenReturn(123);
		assertEquals(repositorySecurityDecorator.add(entities), Integer.valueOf(123));
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void addStreamNoPermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Stream<Entity> entities = Stream.empty();
		try
		{
			repositorySecurityDecorator.add(entities);
		}
		catch (MolgenisDataAccessException e)
		{
			verify(decoratedRepository, times(1)).getName();
			verifyNoMoreInteractions(decoratedRepository);
			throw e;
		}
	}

	@Test
	public void findAllPermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Iterable<Object> ids = Arrays.<Object> asList(Integer.valueOf(0), Integer.valueOf(1));
		Fetch fetch = new Fetch();
		Iterable<Entity> entities = Arrays.asList(mock(Entity.class), mock(Entity.class));
		when(decoratedRepository.findAll(ids, fetch)).thenReturn(entities);
		assertEquals(entities, Lists.newArrayList(repositorySecurityDecorator.findAll(ids, fetch)));
		verify(decoratedRepository, times(1)).findAll(ids, fetch);
	}

	@Test
	public void deleteStream()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_WRITE_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Stream<Entity> entities = Stream.empty();
		repositorySecurityDecorator.delete(entities);
		verify(decoratedRepository, times(1)).delete(entities);
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void deleteStreamNoPermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Stream<Entity> entities = Stream.empty();
		try
		{
			repositorySecurityDecorator.delete(entities);
		}
		catch (MolgenisDataAccessException e)
		{
			verify(decoratedRepository, times(1)).getName();
			verifyNoMoreInteractions(decoratedRepository);
			throw e;
		}
	}

	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	@Test
	public void updateStream()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_WRITE_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Entity entity0 = mock(Entity.class);
		Stream<Entity> entities = Stream.of(entity0);
		ArgumentCaptor<Stream<Entity>> captor = ArgumentCaptor.forClass((Class) Stream.class);
		doNothing().when(decoratedRepository).update(captor.capture());
		repositorySecurityDecorator.update(entities);
		assertEquals(captor.getValue().collect(Collectors.toList()), Arrays.asList(entity0));
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void updateStreamNoPermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Stream<Entity> entities = Stream.empty();
		try
		{
			repositorySecurityDecorator.update(entities);
		}
		catch (MolgenisDataAccessException e)
		{
			verify(decoratedRepository, times(1)).getName();
			verifyNoMoreInteractions(decoratedRepository);
			throw e;
		}
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void findAllNoPermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null);
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Iterable<Object> ids = Arrays.<Object> asList(Integer.valueOf(0), Integer.valueOf(1));
		Fetch fetch = new Fetch();
		Iterable<Entity> entities = Arrays.asList(mock(Entity.class), mock(Entity.class));
		when(decoratedRepository.findAll(ids, fetch)).thenReturn(entities);
		repositorySecurityDecorator.findAll(ids, fetch);
	}

	@Test
	public void findOnePermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_" + entityName.toUpperCase());
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Object id = Integer.valueOf(0);
		Fetch fetch = new Fetch();
		Entity entity = mock(Entity.class);
		when(decoratedRepository.findOne(id, fetch)).thenReturn(entity);
		assertEquals(entity, decoratedRepository.findOne(id, fetch));
		verify(decoratedRepository, times(1)).findOne(id, fetch);
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void findOneNoPermission()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null);
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Object id = Integer.valueOf(0);
		Fetch fetch = new Fetch();
		Entity entity = mock(Entity.class);
		when(decoratedRepository.findOne(id, fetch)).thenReturn(entity);
		repositorySecurityDecorator.findOne(id, fetch);
	}

	@Test
	public void addEntityListener() throws IOException
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_MYENTITY");
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Repository repo = when(mock(Repository.class).getName()).thenReturn("myentity").getMock();

		@SuppressWarnings("resource")
		RepositorySecurityDecorator repoSecurityDecorator = new RepositorySecurityDecorator(repo,
				mock(AppSettings.class));
		repoSecurityDecorator.addEntityListener(mock(EntityListener.class));
	}

	@Test
	public void removeEntityListener()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_MYENTITY");
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Repository repo = when(mock(Repository.class).getName()).thenReturn("myentity").getMock();

		@SuppressWarnings("resource")
		RepositorySecurityDecorator repoSecurityDecorator = new RepositorySecurityDecorator(repo,
				mock(AppSettings.class));
		repoSecurityDecorator.removeEntityListener(mock(EntityListener.class));
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void addEntityListenerNotAllowed() throws IOException
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_MYENTITY");
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Repository repo = when(mock(Repository.class).getName()).thenReturn("yourentity").getMock();

		@SuppressWarnings("resource")
		RepositorySecurityDecorator repoSecurityDecorator = new RepositorySecurityDecorator(repo,
				mock(AppSettings.class));
		repoSecurityDecorator.addEntityListener(mock(EntityListener.class));
	}

	@Test(expectedExceptions = MolgenisDataAccessException.class)
	public void removeEntityListenerNotAllowed()
	{
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("username", null,
				"ROLE_ENTITY_READ_MYENTITY");
		authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Repository repo = when(mock(Repository.class).getName()).thenReturn("yourentity").getMock();

		@SuppressWarnings("resource")
		RepositorySecurityDecorator repoSecurityDecorator = new RepositorySecurityDecorator(repo,
				mock(AppSettings.class));
		repoSecurityDecorator.removeEntityListener(mock(EntityListener.class));
	}
}