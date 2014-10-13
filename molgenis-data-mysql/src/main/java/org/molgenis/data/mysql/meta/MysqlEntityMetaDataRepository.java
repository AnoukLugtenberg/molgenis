package org.molgenis.data.mysql.meta;

import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.ABSTRACT;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.DESCRIPTION;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.EXTENDS;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.FULL_NAME;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.ID_ATTRIBUTE;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.LABEL;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.PACKAGE;
import static org.molgenis.data.mysql.meta.EntityMetaDataMetaData.SIMPLE_NAME;

import java.util.List;

import javax.sql.DataSource;

import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Query;
import org.molgenis.data.mysql.MysqlRepository;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;

import com.google.common.collect.Lists;

class MysqlEntityMetaDataRepository extends MysqlRepository
{
	public static final EntityMetaDataMetaData META_DATA = new EntityMetaDataMetaData();

	public MysqlEntityMetaDataRepository(DataSource dataSource)
	{
		super(dataSource);
		setMetaData(META_DATA);
	}

	public Iterable<EntityMetaData> getEntityMetaDatas()
	{
		List<EntityMetaData> meta = Lists.newArrayList();
		for (Entity entity : this)
		{
			meta.add(toEntityMetaData(entity));
		}

		return meta;
	}

	/**
	 * Gets all EntityMetaData in a package.
	 * 
	 * @param packageName
	 *            the name of the package
	 */
	public List<EntityMetaData> getPackageEntityMetaDatas(String packageName)
	{
		List<EntityMetaData> meta = Lists.newArrayList();
		Query q = new QueryImpl().eq(EntityMetaDataMetaData.PACKAGE, packageName);

		for (Entity entity : findAll(q))
		{
			meta.add(toEntityMetaData(entity));
		}

		return meta;
	}

	/**
	 * Retrieves an EntityMetaData.
	 * 
	 * @param fullyQualifiedName
	 *            the fully qualified name of the entityMetaData
	 * @return the EntityMetaData or null if none found
	 */
	public EntityMetaData getEntityMetaData(String fullyQualifiedName)
	{
		Query q = query().eq(FULL_NAME, fullyQualifiedName);
		Entity entity = findOne(q);
		if (entity == null)
		{
			return null;
		}

		return toEntityMetaData(entity);
	}

	public void addEntityMetaData(EntityMetaData emd)
	{
		Entity entityMetaDataEntity = new MapEntity();
		entityMetaDataEntity.set(FULL_NAME, emd.getName());
		entityMetaDataEntity.set(SIMPLE_NAME, emd.getSimpleName());
		entityMetaDataEntity.set(PACKAGE, emd.getPackage());
		entityMetaDataEntity.set(DESCRIPTION, emd.getDescription());
		entityMetaDataEntity.set(ABSTRACT, emd.isAbstract());
		if (emd.getIdAttribute() != null) entityMetaDataEntity.set(ID_ATTRIBUTE, emd.getIdAttribute().getName());
		entityMetaDataEntity.set(LABEL, emd.getLabel());
		if (emd.getExtends() != null) entityMetaDataEntity.set(EXTENDS, emd.getExtends().getName());

		add(entityMetaDataEntity);
	}

	private DefaultEntityMetaData toEntityMetaData(Entity entity)
	{
		String name = entity.getString(FULL_NAME);
		DefaultEntityMetaData entityMetaData = new DefaultEntityMetaData(name);
		entityMetaData.setAbstract(entity.getBoolean(ABSTRACT));
		entityMetaData.setIdAttribute(entity.getString(ID_ATTRIBUTE));
		entityMetaData.setLabel(entity.getString(LABEL));
		entityMetaData.setDescription(entity.getString(DESCRIPTION));

		// Extends
		String extendsEntityName = entity.getString(EXTENDS);
		if (extendsEntityName != null)
		{
			EntityMetaData extendsEmd = getEntityMetaData(extendsEntityName);
			if (extendsEmd == null) throw new MolgenisDataException("Missing super entity [" + extendsEntityName
					+ "] of entity [" + name + "]");
			entityMetaData.setExtends(extendsEmd);
		}

		return entityMetaData;
	}
}
