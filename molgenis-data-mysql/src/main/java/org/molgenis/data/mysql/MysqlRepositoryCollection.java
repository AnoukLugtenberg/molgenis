package org.molgenis.data.mysql;

import static org.molgenis.MolgenisFieldTypes.BOOL;
import static org.molgenis.MolgenisFieldTypes.INT;
import static org.molgenis.MolgenisFieldTypes.TEXT;
import static org.molgenis.MolgenisFieldTypes.XREF;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AggregateableCrudRepositorySecurityDecorator;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.model.MolgenisModelException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public abstract class MysqlRepositoryCollection implements RepositoryCollection
{
	private final DataSource ds;
	private final DataService dataService;
	private Map<String, MysqlRepository> repositories;
	private MysqlRepository entities;
	private MysqlRepository attributes;

	public MysqlRepositoryCollection(DataSource ds, DataService dataService)
	{
		this.ds = ds;
		this.dataService = dataService;
		refreshRepositories();
	}

	public DataSource getDataSource()
	{
		return ds;
	}

	/**
	 * Return a spring managed prototype bean
	 */
	protected abstract MysqlRepository createMysqlRepsitory();

	public void refreshRepositories()
	{
		repositories = new LinkedHashMap<String, MysqlRepository>();

		DefaultEntityMetaData entitiesMetaData = new DefaultEntityMetaData("entities");
		entitiesMetaData.setIdAttribute("name");
		entitiesMetaData.addAttribute("name").setNillable(false);
		entitiesMetaData.addAttribute("idAttribute");
		entitiesMetaData.addAttribute("abstract").setDataType(BOOL);
		entitiesMetaData.addAttribute("label");
		entitiesMetaData.addAttribute("extends");// TODO create XREF to entityMD when dependency resolving is fixed
		entitiesMetaData.addAttribute("description").setDataType(TEXT);

		entities = createMysqlRepsitory();
		entities.setMetaData(entitiesMetaData);

		DefaultEntityMetaData attributesMetaData = new DefaultEntityMetaData("attributes");
		attributesMetaData.setIdAttribute("identifier");
		attributesMetaData.addAttribute("identifier").setNillable(false).setDataType(INT).setAuto(true);
		attributesMetaData.addAttribute("entity").setNillable(false);
		attributesMetaData.addAttribute("name").setNillable(false);
		attributesMetaData.addAttribute("dataType");
		attributesMetaData.addAttribute("refEntity").setDataType(XREF).setRefEntity(entitiesMetaData);
		attributesMetaData.addAttribute("nillable").setDataType(BOOL);
		attributesMetaData.addAttribute("auto").setDataType(BOOL);
		attributesMetaData.addAttribute("idAttribute").setDataType(BOOL);
		attributesMetaData.addAttribute("lookupAttribute").setDataType(BOOL);
		attributesMetaData.addAttribute("visible").setDataType(BOOL);
		attributesMetaData.addAttribute("label");
		attributesMetaData.addAttribute("description").setDataType(TEXT);
		attributesMetaData.addAttribute("aggregateable").setDataType(BOOL);

		attributes = createMysqlRepsitory();
		attributes.setMetaData(attributesMetaData);

		if (!tableExists("entities"))
		{
			entities.create();

			if (!tableExists("attributes"))
			{
				attributes.create();
			}
		}
		else if (attributes.count() == 0)
		{
			// Update table structure to prevent errors is apps that don't use emx
			attributes.drop();
			entities.drop();
			entities.create();
			attributes.create();
		}

		// Update attributes table if needed
		if (!columnExists("attributes", "aggregateable"))
		{
			String sql;
			try
			{
				sql = attributes.getAlterSql(attributesMetaData.getAttribute("aggregateable"));
			}
			catch (MolgenisModelException e)
			{
				throw new RuntimeException(e);
			}

			new JdbcTemplate(ds).execute(sql);
		}

		Map<String, DefaultEntityMetaData> metadata = new LinkedHashMap<String, DefaultEntityMetaData>();

		// read the attributes
		for (Entity attribute : attributes)
		{
			DefaultEntityMetaData entityMetaData = metadata.get(attribute.getString("entity"));
			if (entityMetaData == null)
			{
				entityMetaData = new DefaultEntityMetaData(attribute.getString("entity"));
				metadata.put(attribute.getString("entity"), entityMetaData);
			}

			DefaultAttributeMetaData attributeMetaData = new DefaultAttributeMetaData(attribute.getString("name"));
			attributeMetaData.setDataType(MolgenisFieldTypes.getType(attribute.getString("dataType")));
			attributeMetaData.setNillable(attribute.getBoolean("nillable"));
			attributeMetaData.setAuto(attribute.getBoolean("auto"));
			attributeMetaData.setIdAttribute(attribute.getBoolean("idAttribute"));
			attributeMetaData.setLookupAttribute(attribute.getBoolean("lookupAttribute"));
			attributeMetaData.setVisible(attribute.getBoolean("visible"));
			attributeMetaData.setLabel(attribute.getString("label"));
			attributeMetaData.setDescription(attribute.getString("description"));
			attributeMetaData.setAggregateable(attribute.getBoolean("aggregateable") == null ? false : attribute
					.getBoolean("aggregateable"));

			entityMetaData.addAttributeMetaData(attributeMetaData);
		}

		// read the entities
		for (Entity entity : entities)
		{
			DefaultEntityMetaData entityMetaData = metadata.get(entity.getString("name"));
			if (entityMetaData == null)
			{
				entityMetaData = new DefaultEntityMetaData(entity.getString("name"));
				metadata.put(entity.getString("name"), entityMetaData);
			}

			entityMetaData.setAbstract(entity.getBoolean("abstract"));
			entityMetaData.setIdAttribute(entity.getString("idAttribute"));
			entityMetaData.setLabel(entity.getString("label"));
			entityMetaData.setDescription(entity.getString("description"));
		}

		// read extends
		for (Entity entity : entities)
		{
			String extendsEntityName = entity.getString("extends");
			if (extendsEntityName != null)
			{
				String entityName = entity.getString("name");
				DefaultEntityMetaData emd = metadata.get(entityName);
				DefaultEntityMetaData extendsEmd = metadata.get(extendsEntityName);
				if (extendsEmd == null) throw new RuntimeException("Missing super entity [" + extendsEntityName
						+ "] of entity [" + entityName + "]");
				emd.setExtends(extendsEmd);
			}
		}

		// read the refEntity
		for (Entity attribute : attributes)
		{
			if (attribute.getString("refEntity") != null)
			{
				EntityMetaData entityMetaData = metadata.get(attribute.getString("entity"));
				DefaultAttributeMetaData attributeMetaData = (DefaultAttributeMetaData) entityMetaData
						.getAttribute(attribute.getString("name"));
				EntityMetaData ref = metadata.get(attribute.getString("refEntity"));
				if (ref == null) throw new RuntimeException("refEntity '" + attribute.getString("refEntity")
						+ "' missing for " + entityMetaData.getName() + "." + attributeMetaData.getName());
				attributeMetaData.setRefEntity(ref);
			}
		}

		// instantiate the repos
		for (EntityMetaData emd : metadata.values())
		{
			if (!emd.isAbstract())
			{
				MysqlRepository repo = createMysqlRepsitory();
				repo.setMetaData(emd);
				repositories.put(emd.getName(), repo);
			}
		}
	}

	private boolean tableExists(String table)
	{
		Connection conn = null;
		try
		{

			conn = ds.getConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet tables = dbm.getTables(null, null, table, null);
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
			catch (Exception e2)
			{
				e2.printStackTrace();
			}
		}
	}

	private boolean columnExists(String table, String column)
	{
		Connection conn = null;
		try
		{

			conn = ds.getConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet columns = dbm.getColumns(null, null, table, column);
			return columns.next();
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
			catch (Exception e2)
			{
				e2.printStackTrace();
			}
		}
	}

	@Transactional
	public MysqlRepository add(EntityMetaData emd)
	{
		MysqlRepository repository = null;

		if (entities.query().eq("name", emd.getName()).count() > 0)
		{
			if (emd.isAbstract())
			{
				return null;
			}

			repository = repositories.get(emd.getName());
			if (repository == null) throw new IllegalStateException("Repository [" + emd.getName()
					+ "] registered in entities table but missing in the MysqlRepositoryCollection");

			if (!dataService.hasRepository(emd.getName()))
			{
				dataService.addRepository(new AggregateableCrudRepositorySecurityDecorator(repository));
			}

			return repository;
		}

		// if not abstract add to repositories
		if (!emd.isAbstract())
		{
			repository = createMysqlRepsitory();
			repository.setMetaData(emd);
			repository.create();

			repositories.put(emd.getName(), repository);
			dataService.addRepository(new AggregateableCrudRepositorySecurityDecorator(repository));
		}

		// Add to entities and attributes tables, this should be done AFTER the creation of new tables because create
		// table statements are ddl statements and when these are executed mysql does an implicit commit. So when the
		// create table fails a rollback does not work anymore
		Entity e = new MapEntity();
		e.set("name", emd.getName());
		e.set("description", emd.getDescription());
		e.set("abstract", emd.isAbstract());
		if (emd.getIdAttribute() != null) e.set("idAttribute", emd.getIdAttribute().getName());
		e.set("label", emd.getLabel());
		if (emd.getExtends() != null) e.set("extends", emd.getExtends().getName());
		entities.add(e);

		// add attribute metadata
		for (AttributeMetaData att : emd.getAttributes())
		{
			addAttribute(emd, att);
		}

		return repository;
	}

	@Transactional
	public void addAttribute(EntityMetaData emd, AttributeMetaData att)
	{
		Entity a = new MapEntity();
		a.set("entity", emd.getName());
		a.set("name", att.getName());
		a.set("defaultValue", att.getDefaultValue());
		a.set("dataType", att.getDataType());
		a.set("idAttribute", att.isIdAtrribute());
		a.set("nillable", att.isNillable());
		a.set("auto", att.isAuto());
		a.set("visible", att.isVisible());
		a.set("label", att.getLabel());
		a.set("description", att.getDescription());
		a.set("aggregateable", att.isAggregateable());

		if (att.getRefEntity() != null) a.set("refEntity", att.getRefEntity().getName());

		boolean lookupAttribute = att.isLookupAttribute();
		if (att.isIdAtrribute())
		{
			lookupAttribute = true;
		}
		a.set("lookupAttribute", lookupAttribute);

		attributes.add(a);
	}

	@Override
	public Iterable<String> getEntityNames()
	{
		return repositories.keySet();
	}

	@Override
	public Repository getRepositoryByEntityName(String name)
	{
		MysqlRepository repo = repositories.get(name);
		if (repo == null)
		{
			return null;
		}

		return new AggregateableCrudRepositorySecurityDecorator(repo);
	}

	public void drop(EntityMetaData md)
	{
		assert md != null;
		drop(md.getName());
	}

	public void drop(String name)
	{
		// remove the repo
		MysqlRepository r = repositories.get(name);
		if (r != null)
		{
			r.drop();
			repositories.remove(name);
			dataService.removeRepository(r.getName());
		}

		// delete metadata
		attributes.delete(attributes.findAll(new QueryImpl().eq("entity", name)));
		entities.delete(entities.findAll(new QueryImpl().eq("name", name)));
	}

	@Transactional
	public void update(EntityMetaData metadata)
	{
		MysqlRepository repository = repositories.get(metadata.getName());
		EntityMetaData entityMetaData = repository.getEntityMetaData();
		for (AttributeMetaData attr : metadata.getAttributes())
		{
			AttributeMetaData currentAttribute = entityMetaData.getAttribute(attr.getName());
			if (currentAttribute != null)
			{
				if (!currentAttribute.getDataType().equals(attr.getDataType()))
				{
					throw new MolgenisDataException("Changing type for existing attributes is not currently supported");
				}
			}
			else if (!attr.isNillable())
			{
				throw new MolgenisDataException("Adding non-nillable attributes is not currently supported");
			}
			else
			{
				addAttribute(metadata, attr);
				DefaultEntityMetaData metaData = (DefaultEntityMetaData) repository.getEntityMetaData();
				metaData.addAttributeMetaData(attr);
				repository.addAttribute(attr);
			}
		}
	}
}
