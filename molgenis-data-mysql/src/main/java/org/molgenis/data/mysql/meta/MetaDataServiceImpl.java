package org.molgenis.data.mysql.meta;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Package;
import org.molgenis.data.meta.WritableMetaDataService;
import org.molgenis.data.mysql.MysqlRepositoryCollection;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class MysqlWritableMetaDataService implements WritableMetaDataService
{
	private MysqlPackageRepository packageRepository;
	private MysqlEntityMetaDataRepository entityMetaDataRepository;
	private MysqlAttributeMetaDataRepository attributeMetaDataRepository;

	/**
	 * Setter for the MysqlRepositoryCollection, to be called after it's created. This resolves the circular dependency
	 * {@link MysqlRepositoryCollection} => decorated {@link WritableMetaDataService} =>
	 * {@link MysqlRepositoryCollection}
	 * 
	 * @param mysqlRepositoryCollection
	 */
	public void setRepositoryCollection(MysqlRepositoryCollection repositoryCollection)
	{
		if (repositoryCollection != null)
		{
			packageRepository = new MysqlPackageRepository(repositoryCollection);
			entityMetaDataRepository = new MysqlEntityMetaDataRepository(repositoryCollection);
			attributeMetaDataRepository = new MysqlAttributeMetaDataRepository(repositoryCollection);
		}
	}

	@Override
	public Set<EntityMetaData> getEntityMetaDatas()
	{
		Map<String, EntityMetaData> metadata = Maps.newLinkedHashMap();

		// read the entity meta data
		for (EntityMetaData entityMetaData : entityMetaDataRepository.getEntityMetaDatas())
		{
			DefaultEntityMetaData entityMetaDataWithAttributes = new DefaultEntityMetaData(entityMetaData);
			metadata.put(entityMetaDataWithAttributes.getName(), entityMetaDataWithAttributes);

			// add the attribute meta data of the entity
			for (AttributeMetaData attributeMetaData : attributeMetaDataRepository
					.findForEntity(entityMetaDataWithAttributes.getName()))
			{
				entityMetaDataWithAttributes.addAttributeMetaData(attributeMetaData);
			}
		}

		// read the refEntity
		for (Entity attribute : attributeMetaDataRepository.getAttributeEntities())
		{
			if (attribute.getString(AttributeMetaDataMetaData.REF_ENTITY) != null)
			{
				EntityMetaData entityMetaData = metadata.get(attribute.getString(AttributeMetaDataMetaData.ENTITY));
				DefaultAttributeMetaData attributeMetaData = (DefaultAttributeMetaData) entityMetaData
						.getAttribute(attribute.getString(AttributeMetaDataMetaData.NAME));
				EntityMetaData ref = metadata.get(attribute.getString(AttributeMetaDataMetaData.REF_ENTITY));
				if (ref == null) throw new RuntimeException("refEntity '" + attribute.getString("refEntity")
						+ "' missing for " + entityMetaData.getName() + "." + attributeMetaData.getName());
				attributeMetaData.setRefEntity(ref);
			}
		}

		Set<EntityMetaData> metadataSet = Sets.newLinkedHashSet();
		metadataSet.add(MysqlPackageRepository.META_DATA);
		metadataSet.add(MysqlEntityMetaDataRepository.META_DATA);
		metadataSet.add(MysqlAttributeMetaDataRepository.META_DATA);

		for (String name : metadata.keySet())
		{
			metadataSet.add(metadata.get(name));
		}

		return metadataSet;
	}

	@Override
	public void removeEntityMetaData(String entityName)
	{
		attributeMetaDataRepository.deleteAllAttributes(entityName);
		entityMetaDataRepository.delete(entityName);
	}

	@Override
	public void removeAttributeMetaData(String entityName, String attributeName)
	{
		// Update AttributeMetaDataRepository
		attributeMetaDataRepository.remove(entityName, attributeName);
	}

	@Override
	public void addEntityMetaData(EntityMetaData emd)
	{
		if (attributeMetaDataRepository == null)
		{
			return;
		}

		packageRepository.add(emd.getPackage());

		entityMetaDataRepository.add(emd);

		// add attribute metadata
		for (AttributeMetaData att : emd.getAttributes())
		{
			attributeMetaDataRepository.add(emd.getName(), att);
		}
	}

	@Override
	public void addAttributeMetaData(String name, AttributeMetaData attr)
	{
		attributeMetaDataRepository.add(name, attr);
	}

	@Override
	public Iterable<AttributeMetaData> getEntityAttributeMetaData(String entityName)
	{
		return attributeMetaDataRepository.findForEntity(entityName);
	}

	@Override
	public EntityMetaData getEntityMetaData(String fullyQualifiedName)
	{
		// at construction time, will be called when entityMetaDataRepository is still null
		if (attributeMetaDataRepository == null)
		{
			return null;
		}
		return entityMetaDataRepository.getEntityMetaData(fullyQualifiedName);
	}

	@Override
	public void addPackage(Package p)
	{
		packageRepository.add(p);
	}

	@Override
	public List<EntityMetaData> getPackageEntityMetaDatas(String packageName)
	{
		return entityMetaDataRepository.getPackageEntityMetaDatas(packageName);
	}

	@Override
	public Package getPackage(String string)
	{
		return packageRepository.getPackage(string);
	}

	@Override
	public Iterable<Package> getPackages()
	{
		return packageRepository.getPackages();
	}

	/**
	 * Empties all metadata tables for the sake of testability.
	 */
	public void recreateMetaDataRepositories()
	{
		attributeMetaDataRepository.deleteAll();
		entityMetaDataRepository.deleteAll();
		packageRepository.deleteAll();
		packageRepository.addDefaultPackage();
	}
}