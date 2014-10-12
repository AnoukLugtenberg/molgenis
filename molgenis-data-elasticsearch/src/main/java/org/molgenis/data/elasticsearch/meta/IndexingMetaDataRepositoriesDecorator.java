package org.molgenis.data.elasticsearch.meta;

import java.io.IOException;
import java.util.List;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Package;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.meta.MetaDataRepositories;
import org.springframework.transaction.annotation.Transactional;

/**
 * Meta data repository for attributes that wraps an existing repository
 */
public class IndexingMetaDataRepositoriesDecorator implements MetaDataRepositories
{
	private final MetaDataRepositories delegate;
	private final DataService dataService;
	private final SearchService elasticSearchService;

	public IndexingMetaDataRepositoriesDecorator(MetaDataRepositories delegate, DataService dataService,
			SearchService elasticSearchService)
	{
		if (delegate == null) throw new IllegalArgumentException("metaDataRepositories is null");
		if (dataService == null) throw new IllegalArgumentException("dataService is null");
		if (elasticSearchService == null) throw new IllegalArgumentException("elasticSearchService is null");
		this.delegate = delegate;
		this.dataService = dataService;
		this.elasticSearchService = elasticSearchService;
	}

	@Override
	public Iterable<AttributeMetaData> getEntityAttributeMetaData(String entityName)
	{
		return delegate.getEntityAttributeMetaData(entityName);
	}

	@Override
	@Transactional
	public void addAttributeMetaData(String entityName, AttributeMetaData attribute)
	{
		delegate.addAttributeMetaData(entityName, attribute);
		updateMappings(entityName);
	}

	@Override
	@Transactional
	public void removeAttributeMetaData(String entityName, String attributeName)
	{
		delegate.removeAttributeMetaData(entityName, attributeName);
		updateMappings(entityName);
	}

	private void updateMappings(String entityName)
	{
		try
		{
			elasticSearchService.createMappings(dataService.getEntityMetaData(entityName));
		}
		catch (IOException e)
		{
			throw new MolgenisDataException(e);
		}
	}

	@Override
	@Transactional
	public void addEntityMetaData(EntityMetaData entityMetaData)
	{
		delegate.addEntityMetaData(entityMetaData);

		try
		{
			elasticSearchService.createMappings(entityMetaData);
		}
		catch (IOException e)
		{
			throw new MolgenisDataException(e);
		}
	}

	@Override
	public Iterable<EntityMetaData> getEntityMetaDatas()
	{
		return delegate.getEntityMetaDatas();
	}

	@Override
	public boolean hasEntity(EntityMetaData entityMetaData)
	{
		return delegate.hasEntity(entityMetaData);
	}

	@Override
	public void createAndUpgradeMetaDataTables()
	{
		delegate.createAndUpgradeMetaDataTables();
	}

	@Override
	public void removeEntityMetaData(String name)
	{
		delegate.removeEntityMetaData(name);
		elasticSearchService.delete(name);
	}

	@Override
	public Iterable<Package> getPackages()
	{
		return delegate.getPackages();
	}

	@Override
	public Package getPackage(String name)
	{
		return delegate.getPackage(name);
	}

	@Override
	public List<EntityMetaData> getPackageEntityMetaDatas(String packageName)
	{
		return delegate.getPackageEntityMetaDatas(packageName);
	}

	@Override
	public EntityMetaData getEntityMetaData(String name)
	{
		return delegate.getEntityMetaData(name);
	}

	@Override
	public void addPackage(Package p)
	{
		delegate.addPackage(p);
	}

}
