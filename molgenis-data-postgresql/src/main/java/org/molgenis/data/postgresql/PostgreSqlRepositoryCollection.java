package org.molgenis.data.postgresql;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.postgresql.PostgreSqlQueryUtils.getTableName;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.molgenis.data.Entity;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.Repository;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.meta.EntityMetaDataImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public abstract class PostgreSqlRepositoryCollection implements RepositoryCollection
{
	public static final String NAME = "PostgreSQL";

	private final DataSource dataSource;

	private final Map<String, PostgreSqlRepository> repositories = new LinkedHashMap<>();

	@Autowired
	public PostgreSqlRepositoryCollection(DataSource dataSource)
	{
		this.dataSource = requireNonNull(dataSource);
	}


	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Repository<Entity> createRepository(EntityMetaData entityMeta)
	{
		PostgreSqlRepository repository = createPostgreSqlRepository();
		repository.setMetaData(entityMeta);
		if (!isTableExists(entityMeta))
		{
			repository.create();
		}
		repositories.put(entityMeta.getName(), repository);

		return repository;
	}

	@Override
	public boolean hasRepository(EntityMetaData entityMeta)
	{
		return isTableExists(entityMeta);
	}

	@Override
	public Iterable<String> getEntityNames()
	{
		return repositories.keySet();
	}

	@Override
	public Repository<Entity> getRepository(String name)
	{
		return repositories.get(name);
	}

	@Override
	public Repository<Entity> getRepository(EntityMetaData entityMeta)
	{
		PostgreSqlRepository repository = createPostgreSqlRepository();
		repository.setMetaData(entityMeta);
		return repository;
	}

	@Override
	public Iterator<Repository<Entity>> iterator()
	{
		return Iterators.transform(repositories.values().iterator(), new Function<PostgreSqlRepository, Repository<Entity>>()
		{
			@Override
			public Repository<Entity> apply(PostgreSqlRepository repo)
			{
				return repo;
			}
		});
	}

	@Override
	public void deleteRepository(String entityName)
	{
		PostgreSqlRepository repo = repositories.get(entityName);
		if (repo != null)
		{
			repo.drop();
			repositories.remove(entityName);
		}
	}

	@Override
	public void addAttribute(String entityName, AttributeMetaData attribute)
	{
		PostgreSqlRepository repo = repositories.get(entityName);
		if (repo == null)
		{
			throw new UnknownEntityException(String.format("Unknown entity '%s'", entityName));
		}
		repo.addAttribute(attribute);
	}

	@Override
	public void deleteAttribute(String entityName, String attributeName)
	{
		PostgreSqlRepository repo = repositories.get(entityName);
		if (repo == null)
		{
			throw new UnknownEntityException(String.format("Unknown entity '%s'", entityName));
		}
		repo.dropAttribute(attributeName);
	}

	/**
	 * Return a spring managed prototype bean
	 */
	protected abstract PostgreSqlRepository createPostgreSqlRepository();

	private boolean isTableExists(EntityMetaData entityMeta)
	{
		Connection conn = null;
		try
		{
			conn = dataSource.getConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			// DatabaseMetaData.getTables() requires table name without double quotes, only search TABLE table type to
			// avoid matches with system tables
			ResultSet tables = dbm.getTables(null, null, getTableName(entityMeta, false), new String[]
			{ "TABLE" });
			return tables.next();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}