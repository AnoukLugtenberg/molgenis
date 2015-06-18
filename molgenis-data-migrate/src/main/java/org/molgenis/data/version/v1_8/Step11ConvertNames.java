package org.molgenis.data.version.v1_8;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.molgenis.data.version.MolgenisUpgrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Part of migration to MOLGENIS 1.8.
 * 
 * Package/entity/attribute names must now bow to the following rules: 1. The only characters allowed are [a-zA-Z0-9_#].
 * 2. Names may not begin with digits. 3. The maximum length is 30 characters. 4. Keywords reserved by Java, JavaScript
 * and MySQL can not be used.
 * 
 * This migration script finds all invalid names within the MySQL back-end and changes them to valid names.
 * 
 * @author tommy
 */
public class Step11ConvertNames extends MolgenisUpgrade
{
	private JdbcTemplate template;
	private DataSource dataSource;
	private static final Logger LOG = LoggerFactory.getLogger(Step11ConvertNames.class);

	public static final String OLD_DEFAULT_PACKAGE = "default";
	public static final String NEW_DEFAULT_PACKAGE = "base";

	private HashMap<String, String> packageNameChanges = new HashMap<>();
	private HashMap<String, String> entityNameChanges = new HashMap<>();
	private HashMap<String, HashMap<String, String>> attributeNameChanges = new HashMap<>();
	private HashMap<String, HashMap<String, String>> mrefNameChanges = new HashMap<>();
	private HashMap<String, String> mrefNoChanges = new HashMap<>();
	private HashMap<String, List<String>> entitiesAttributesIds = new HashMap<>();

	public Step11ConvertNames(SingleConnectionDataSource dataSource)
	{
		super(10, 11);

		// InnoDB only allows setting foreign_key_checks=0 for a single session, so use a single connection source
		this.template = new JdbcTemplate(dataSource);

		this.dataSource = dataSource;
	}

	@Override
	public void upgrade()
	{
		LOG.info("Validating JPA entities...");
		checkJPAentities();

		setForeignKeyConstraintCheck(false);

		LOG.info("Validating package names...");
		checkAndUpdatePackages(null, null);

		LOG.info("Validating entity names...");
		checkAndUpdateEntities();

		LOG.info("Validating attribute names...");
		checkAndUpdateAttributes();

		LOG.info("Updating tags...");
		updateTags();

		LOG.info("Updating MySQL tables...");
		updateEntityTables();

		setForeignKeyConstraintCheck(true);

		try
		{
			// manually close the single datasource connection
			template.getDataSource().getConnection().close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		// throw new RuntimeException("EXIT");
	}

	/** Updates the names of columns in (mref)tables and updates the table names themselves. */
	public void updateEntityTables()
	{
		// get a list of all the tables
		HashSet<String> tableNames = Sets.newHashSet();
		List<Map<String, Object>> tables = template.queryForList("SHOW TABLES");

		// The name of this column (Tables-in-omx) depends on the database name, workaround:
		tables.forEach(row -> tableNames.add(row.get(row.keySet().iterator().next()).toString()));

		// get the fullNames of all the entities that have changes (in entity or attribute names)
		HashSet<String> entitiesToChange = Sets.newHashSet();
		entitiesToChange.addAll(entityNameChanges.keySet());
		entitiesToChange.addAll(attributeNameChanges.keySet());

		// update all mref tables that have a change in the mref name
		for (Map.Entry<String, HashMap<String, String>> mrefTable : mrefNameChanges.entrySet())
		{
			// mref tables only have one column we need to change, so just get the first attr
			Map.Entry<String, String> attributeChange = mrefTable.getValue().entrySet().iterator().next();

			// we can't update a column name without specifying its type, so retrieve the type first
			List<Map<String, Object>> column = template.queryForList(String.format(
					"SHOW COLUMNS FROM `%s` WHERE Field='%s'", mrefTable.getKey(), attributeChange.getKey()));

			String type = column.iterator().next().get("Type").toString();

			template.execute(String.format("ALTER TABLE `%s` CHANGE `%s` `%s` %s", mrefTable.getKey(),
					attributeChange.getKey(), attributeChange.getValue(), type));

			// we're done with the attributes of this mref table, now fix the table name itself
			String entityName = mrefTable.getKey().replaceAll("_" + attributeChange.getKey() + "$", "");
			if (entityNameChanges.containsKey(entityName)) entityName = entityNameChanges.get(entityName);
			String newTableName = String.format("%s_%s", entityName, attributeChange.getValue());

			template.execute(String.format("RENAME TABLE `%s` TO `%s`", mrefTable.getKey(), newTableName));
		}

		// update the rest of the mref tables
		for (Map.Entry<String, String> mrefTable : mrefNoChanges.entrySet())
		{
			String entityName = mrefTable.getKey().replaceAll("_" + mrefTable.getValue() + "$", "");
			if (entityNameChanges.containsKey(entityName))
			{
				entityName = entityNameChanges.get(entityName);
			}
			else
			{
				continue;
			}
			String newTableName = String.format("%s_%s", entityName, mrefTable.getValue());

			template.execute(String.format("RENAME TABLE `%s` TO `%s`", mrefTable.getKey(), newTableName));
		}

		// update rest of entities
		for (String entity : entitiesToChange)
		{
			// check if we need to change the attribute names
			if (attributeNameChanges.containsKey(entity))
			{
				for (Map.Entry<String, String> attributeChange : attributeNameChanges.get(entity).entrySet())
				{
					// we can't update a column name without specifying its type, so retrieve the type first
					List<Map<String, Object>> column = template.queryForList(String.format(
							"SHOW COLUMNS FROM `%s` WHERE Field='%s'", entity, attributeChange.getKey()));

					String type = column.iterator().next().get("Type").toString();

					template.execute(String.format("ALTER TABLE `%s` CHANGE `%s` `%s` %s", entity,
							attributeChange.getKey(), attributeChange.getValue(), type));
				}
			}

			// check if we need to change the entity name
			if (entityNameChanges.containsKey(entity))
			{
				template.execute(String.format("RENAME TABLE `%s` TO `%s`", entity, entityNameChanges.get(entity)));
			}
		}
	}

	/**
	 * Updates the fullNames in the *_tags tables.
	 */
	public void updateTags()
	{
		// attributes_tags doesn't need change
		// tags doesn't need change

		// entities_tags has fullNames, which need to be updated
		List<Map<String, Object>> entityTags = template.queryForList("SELECT fullName FROM entities_tags");
		for (Map<String, Object> tag : entityTags)
		{
			String fullName = tag.get("fullName").toString();

			if (entityNameChanges.containsKey(fullName))
			{
				template.execute(String.format("UPDATE entities_tags SET fullName='%s' WHERE fullName='%s'",
						entityNameChanges.get(fullName), fullName));
			}
		}

		// packages_tags has fullNames, which need to be updated
		List<Map<String, Object>> packagesTags = template.queryForList("SELECT fullName FROM packages_tags");
		for (Map<String, Object> tag : packagesTags)
		{
			String fullName = tag.get("fullName").toString();

			if (entityNameChanges.containsKey(fullName))
			{
				template.execute(String.format("UPDATE packages_tags SET fullName='%s' WHERE fullName='%s'",
						entityNameChanges.get(fullName), fullName));
			}
		}
	}

	/**
	 * Updates attribute names and refEntity references. Attribute names must be unique per entity, so this method
	 * builds a scope per entity by iterating over the (nested) attributes.
	 */
	public void checkAndUpdateAttributes()
	{
		List<Map<String, Object>> entityAttributes = template.queryForList("SELECT * FROM entities_attributes");

		// instead of doing multiple queries, we'll build a map of entity-attribute relations
		for (Map<String, Object> entityAttribute : entityAttributes)
		{
			String entityFullName = entityAttribute.get("fullName").toString();
			String entityAttributeId = entityAttribute.get("attributes").toString();
			if (entitiesAttributesIds.containsKey(entityFullName))
			{
				entitiesAttributesIds.get(entityFullName).add(entityAttributeId);
			}
			else
			{
				entitiesAttributesIds.put(entityFullName, Lists.newArrayList(entityAttributeId));
			}
		}

		// now use this map to update every entities' attributes
		for (Map.Entry<String, List<String>> entity : entitiesAttributesIds.entrySet())
		{
			List<String> allAttributeIds = getAllAttributeIdsRecursive(entity.getValue());

			List<Map<String, Object>> attributes = template.queryForList(getAttributesByIdSql(allAttributeIds));

			// build scope for this entity
			HashSet<String> scope = Sets.newHashSet();
			attributes.forEach(attribute -> scope.add(attribute.get("name").toString()));

			for (Map<String, Object> attribute : attributes)
			{
				String identifier = attribute.get("identifier").toString();
				String name = attribute.get("name").toString();
				String dataType = attribute.get("dataType").toString();

				String refEntity = null;
				if (!(attribute.get("refEntity") == null)) refEntity = attribute.get("refEntity").toString();

				String newName = fixName(name);
				if (!name.equals(newName))
				{
					// attribute name did not validate, check if it's unique
					if (scope.contains(newName))
					{
						newName = makeNameUnique(newName, scope);
					}

					LOG.info(String.format("In Entity [%s]: Attribute name [%s] is not valid. Changing to [%s]...",
							entity.getKey(), name, newName));

					// store the attribute name changes with the entity they belong to as a key
					// we don't need to store compounds because they only live in the attributes table
					// store mref attributes in a separate map for convenience later on
					if (!dataType.equals("compound"))
					{
						if (dataType.equals("mref") || dataType.equals("categoricalmref"))
						{
							String mrefTableName = String.format("%s_%s", entity.getKey(), name);
							if (!mrefNameChanges.containsKey(mrefTableName))
							{
								mrefNameChanges.put(mrefTableName, Maps.newHashMap());
							}
							mrefNameChanges.get(mrefTableName).put(name, newName);
						}
						else
						{
							if (!attributeNameChanges.containsKey(entity.getKey()))
							{
								attributeNameChanges.put(entity.getKey(), Maps.newHashMap());
							}
							attributeNameChanges.get(entity.getKey()).put(name, newName);
						}
					}
				}
				else
				{
					// we also need to store the names of mrefs that didn't change, because there are cases where we DO
					// need to fix the entity prefix for the mref tables
					if (dataType.equals("mref") || dataType.equals("categoricalmref"))
					{
						String mrefTableName = String.format("%s_%s", entity.getKey(), name);
						mrefNoChanges.put(mrefTableName, newName);
					}
				}

				// get the (new) name for refEntity
				String newRefEntity = refEntity;
				if (entityNameChanges.containsKey(refEntity))
				{
					newRefEntity = entityNameChanges.get(refEntity);
				}

				template.execute(getUpdateAttributeNameAndRefEntity(identifier, newName, newRefEntity));
			}

			// finally, update the expressions for this entity's attributes
			updateAttributeExpressions(attributes, attributeNameChanges.get(entity.getKey()));

			// update the fullNames of the entities to the new names we generated before (in entities_attributes)
			if (entityNameChanges.containsKey(entity.getKey()))
			{
				template.execute(getUpdateEntitiesAttributesFullName(entity.getKey(),
						entityNameChanges.get(entity.getKey())));
			}
		}
	}

	/**
	 * Updates attribute names in expressions. Attribute expression can refer to attributes within the same entity, so
	 * we pass this method an entity-scoped map of changes.
	 */
	public void updateAttributeExpressions(List<Map<String, Object>> attributes, HashMap<String, String> nameChanges)
	{
		if (nameChanges == null) return;

		for (Map<String, Object> attribute : attributes)
		{
			String identifier = attribute.get("identifier").toString();
			String name = attribute.get("name").toString();

			HashMap<String, String> expressions = Maps.newHashMap();
			if (attribute.get("expression") != null)
			{
				expressions.put("expression", attribute.get("expression").toString());
			}
			if (attribute.get("visibleExpression") != null)
			{
				expressions.put("visibleExpression", attribute.get("visibleExpression").toString());
			}
			if (attribute.get("validationExpression") != null)
			{
				expressions.put("validationExpression", attribute.get("validationExpression").toString());
			}

			// check each expression column for attribute names
			for (Map.Entry<String, String> expr : expressions.entrySet())
			{
				String expression = expr.getValue();
				String newExpression = expression;

				// interesting parts in expressions look like this: $('attributeName')
				Pattern pattern = Pattern.compile("\\$\\('(.*)'\\)");
				Matcher matcher = pattern.matcher(expression);
				boolean changesFound = false;
				while (matcher.find())
				{
					// for each occurence of an attribute name, we check if it changed and update the expression
					String exprAttr = matcher.group(1);
					if (nameChanges.containsKey(exprAttr))
					{
						newExpression = newExpression.replaceAll("\\$\\('" + exprAttr + "'\\)",
								"\\$\\('" + nameChanges.get(exprAttr) + "'\\)");

						changesFound = true;
					}
				}

				if (changesFound)
				{
					LOG.info(String.format("In attribute [%s]: %s is not valid, changing from [%s] to [%s]", name,
							expr.getKey(), expression, newExpression));
				}

				// update the (validation|visible)expression column
				template.execute(String.format("UPDATE attributes SET %s=\"%s\" WHERE identifier='%s'", expr.getKey(),
						newExpression, identifier));
			}
		}
	}

	/**
	 * Recursively gets a list of identifiers for all attributes belonging to an entity (including compounds).
	 */
	public List<String> getAllAttributeIdsRecursive(List<String> partialIds)
	{
		List<String> ids = Lists.newArrayList(partialIds);

		for (String id : partialIds)
		{
			// get values for this attribute id
			Map<String, Object> attribute = template.queryForMap(String.format(
					"SELECT identifier, dataType FROM attributes WHERE identifier = '%s'", id));

			if (attribute.get("dataType").toString().equals("compound"))
			{
				// possibly more attributes below this one
				List<Map<String, Object>> attributeParts = template.queryForList(String.format(
						"SELECT * FROM attributes_parts WHERE identifier='%s'", id));

				// get the ids of these attributes and go deeper into the recursion
				List<String> partIds = Lists.newArrayList();
				attributeParts.forEach(part -> partIds.add(part.get("parts").toString()));
				ids.addAll(getAllAttributeIdsRecursive(partIds));
			}
		}

		return ids;
	}

	/**
	 * Updates the entities table by validating simpleName and using the name changes generated in
	 * checkAndUpdatePackages().
	 */
	public void checkAndUpdateEntities()
	{
		List<Map<String, Object>> entities = template
				.queryForList("SELECT fullName, simpleName, package, extends, abstract FROM entities;");

		// build package scopes
		HashMap<String, HashSet<String>> scopes = Maps.newHashMap();
		for (Map<String, Object> entity : entities)
		{
			String pack = entity.get("package").toString();
			String simpleName = entity.get("simpleName").toString();

			if (scopes.containsKey(pack))
			{
				scopes.get(pack).add(simpleName);
			}
			else
			{
				scopes.put(pack, Sets.newHashSet(simpleName));
			}
		}

		// update entities
		for (Map<String, Object> entity : entities)
		{
			// check the name of each entity
			String fullName = entity.get("fullName").toString();
			String simpleName = entity.get("simpleName").toString();
			String pack = entity.get("package").toString();

			// generate a new simpleName if needed
			String newSimpleName = fixName(simpleName);
			if (!simpleName.equals(newSimpleName))
			{
				// name didn't validate, make it unique in its package
				if (scopes.get(pack).contains(newSimpleName))
				{
					newSimpleName = makeNameUnique(newSimpleName, scopes.get(pack));
				}

				LOG.info(String.format("Entity name [%s] is not valid. Changing to [%s]...", simpleName, newSimpleName));
			}

			String newPackage = pack;
			if (fullName.equals(simpleName))
			{
				// special case: if simple name is the same as fullname, we are in the default package, which should be
				// renamed to 'base'
				newPackage = "base";
			}
			else
			{
				// use the newly generated package name if it was changed
				if (packageNameChanges.containsKey(pack))
				{
					newPackage = packageNameChanges.get(pack);
				}
			}

			// generate a new fullName based on the (new) package and (new) simpleName
			String newFullName;
			if (pack.equals(OLD_DEFAULT_PACKAGE))
			{
				// default package is not used as a prefix in the fully qualified name
				newFullName = newSimpleName;
			}
			else
			{
				newFullName = newPackage + "_" + newSimpleName;
			}

			// update the 'store' if we've changed the name (don't care about abstracts)
			if (!fullName.equals(newFullName) && !((boolean) entity.get("abstract")))
			{
				entityNameChanges.put(fullName, newFullName);
			}

			template.execute(getUpdateEntityNamesSql(newFullName, newSimpleName, newPackage, fullName));

		}

		// iterate over the entities one more time to update the extends property with the newly generated fullNames
		for (Map<String, Object> entity : entities)
		{
			if (entity.get("extends") == null) continue;

			String newFullName = entity.get("fullName").toString();
			String extendz = entity.get("extends").toString();
			if (entityNameChanges.containsKey(extendz))
			{
				String newExtends = entityNameChanges.get(extendz);
				template.execute(getUpdateEntityExtendsSql(newFullName, newExtends));
			}
		}
	}

	/**
	 * Recursively checks the names of packages and changes the names when they are not valid.
	 */
	public void checkAndUpdatePackages(String parent, String parentFix)
	{
		List<Map<String, Object>> packages = template.queryForList(getPackagesWithParentSql(parent));
		if (packages.isEmpty()) return;

		// first add packages in this package to the scope
		HashSet<String> scope = Sets.newHashSet();
		packages.forEach(pack -> {
			scope.add(pack.get("name").toString());
			if (pack.get("name").toString().equals(NEW_DEFAULT_PACKAGE)) throw new RuntimeException("EXIT!");
		});

		// iterate over the packages and check the names
		for (Map<String, Object> pack : packages)
		{
			String name = pack.get("name").toString();
			String nameFix = fixName(name);

			String fullName = pack.get("fullName").toString();
			String fullNameFix = fullName;

			// rebuild the full name based on (new) names of the parent and this one
			if (parentFix != null) fullNameFix = String.format("%s_%s", parentFix, nameFix);

			if (!name.equals(nameFix))
			{
				if (name.equals(OLD_DEFAULT_PACKAGE))
				{
					// special case: the 'default' package is renamed to 'base'. The 'base' package is added before the
					// migration starts, so just remove the old 'default' package
					LOG.info("Removing old default package (is now called 'base')");
					template.execute("DELETE FROM packages WHERE fullName = 'default'");
					continue;
				}

				// name wasn't valid, make sure the new one is unique for this scope
				if (scope.contains(nameFix))
				{
					nameFix = makeNameUnique(nameFix, scope);
				}

				LOG.info(String.format("In Package [%s]: package name [%s] is not valid. Changing to [%s]...", parent,
						name, nameFix));

				// update the scope with the new name
				scope.remove(name);
				scope.add(nameFix);

				// change the full package name
				// TODO: fullNameFix sometimes changes based on parent
				fullNameFix = fullNameFix.replaceAll(name + "$", nameFix);

				// update fullname, name and parent in database
				template.execute(getUpdatePackageNamesSql(fullName, fullNameFix, nameFix, parentFix));

				// add the name changes to the store so we can use it later to update all other tables
				packageNameChanges.put(fullName, fullNameFix);
				checkAndUpdatePackages(fullName, fullNameFix);
			}
			else
			{
				// nothing changed in this name, only update the parent
				template.execute(getUpdatePackageNamesSql(fullName, fullNameFix, nameFix, parentFix));

				checkAndUpdatePackages(fullName, fullNameFix);
			}
		}
	}

	public String getUpdateEntityExtendsSql(String newFullName, String newExtends)
	{
		return String.format("UPDATE entities SET extends='%s' WHERE fullName='%s'", newExtends, newFullName);
	}

	public String getUpdateEntityNamesSql(String newFullName, String newSimpleName, String newPackage,
			String oldFullName)
	{
		return String.format("UPDATE entities SET fullName='%s', simpleName='%s', package='%s'WHERE fullName='%s'",
				newFullName, newSimpleName, newPackage, oldFullName);
	}

	public String getPackagesWithParentSql(String parentFullName)
	{
		String query = (parentFullName == null) ? "IS NULL" : String.format("= '%s'", parentFullName);

		return String.format("SELECT fullName, name, parent FROM packages WHERE parent %s;", query);
	}

	public String getUpdatePackageParentNameSql(String parent, String fullName)
	{
		parent = (parent == null) ? "NULL" : String.format("'%s'", parent);
		return String.format("UPDATE packages SET parent=%s WHERE fullName='%s';", parent, fullName);
	}

	public String getUpdatePackageNamesSql(String fullName, String fullNameFix, String nameFix, String parent)
	{
		parent = (parent == null) ? "NULL" : String.format("'%s'", parent);
		return String.format("UPDATE packages SET fullName='%s', name='%s', parent=%s WHERE fullName='%s';",
				fullNameFix, nameFix, parent, fullName);
	}

	public String getUpdateAttributeNameAndRefEntity(String identifier, String newName, String newRefEntity)
	{
		newRefEntity = (newRefEntity == null) ? "NULL" : String.format("'%s'", newRefEntity);
		return String.format("UPDATE attributes SET name='%s', refEntity=%s WHERE identifier='%s'", newName,
				newRefEntity, identifier);
	}

	public String getUpdateEntitiesAttributesFullName(String fullName, String newFullName)
	{
		return String.format("UPDATE entities_attributes SET fullName='%s' WHERE fullName='%s'", newFullName, fullName);
	}

	public String fetchAttributeNamesForEntitySql(String fullyQualifiedEntityName)
	{
		return String
				.format("SELECT attributes.identifier, attributes.name, attributes.refEntity FROM attributes INNER JOIN entities_attributes ON entities_attributes.attributes = attributes.identifier WHERE entities_attributes.fullName = '%s'",
						fullyQualifiedEntityName);
	}

	public String getAttributesByIdSql(List<String> ids)
	{
		if (ids.isEmpty()) return null;

		StringBuilder sb = new StringBuilder(
				"SELECT identifier, name, refEntity, dataType, expression, visibleExpression, validationExpression FROM attributes WHERE ");
		Iterator<String> it = ids.iterator();
		while (it.hasNext())
		{
			sb.append("identifier = '").append(it.next()).append("'");
			if (it.hasNext())
			{
				sb.append(" OR ");
			}
		}

		return sb.toString();
	}

	/**
	 * Turns the MySQL foreign key check on or off.
	 */
	public void setForeignKeyConstraintCheck(boolean doCheck)
	{
		int check = (doCheck == true) ? 1 : 0;
		template.execute(String.format("SET FOREIGN_KEY_CHECKS = %d;", check));
	}

	/**
	 * Checks the JPA entities and totally freaks out when they don't validate. Migration is 'impossible' for these
	 * entities, so we stop the application and leave it to the user to change their models.
	 */
	public void checkJPAentities() throws RuntimeException
	{
		List<Map<String, Object>> jpaEntities = template
				.queryForList("SELECT fullName FROM entities WHERE backend = 'JPA'");

		for (Map<String, Object> jpaEntity : jpaEntities)
		{
			// check name of this JPA entity
			String entityName = jpaEntity.get("fullName").toString();

			if (!entityName.equals(fixName(entityName)))
			{
				throw new RuntimeException(
						"The JPA entity ["
								+ entityName
								+ "] did not pass validation. JPA entities cannot be automatically migrated. Please update the entities manually and rebuild the app.");
			}

			// check attributes of this JPA entity
			List<Map<String, Object>> jpaEntityAttributes = template
					.queryForList(fetchAttributeNamesForEntitySql(entityName));

			for (Map<String, Object> attribute : jpaEntityAttributes)
			{
				String attributeName = attribute.get("name").toString();

				if (!attributeName.equals(fixName(attributeName)))
				{
					throw new RuntimeException(
							"The attribute ["
									+ attributeName
									+ "] of JPA entity ["
									+ entityName
									+ "] did not pass validation. JPA entities cannot be automatically migrated. Please update the entities manually and rebuild the app.");
				}
			}
		}
	}

	/**
	 * Changes invalid names using these rules: 1. Strip invalid characters and digits at the start of a name. 2. Append
	 * a '0' if a name is a reserved keyword. 3. Truncate long names so they are no longer than 30.
	 */
	public String fixName(String name)
	{
		// remove all invalid characters
		name = name.replaceAll("[^a-zA-Z0-9_#]+", "");

		// strip digits at start of string
		name = name.replaceAll("^[0-9]+", "");

		// truncate names longer than 30
		if (name.length() > MAX_NAME_LENGTH)
		{
			name = name.substring(0, MAX_NAME_LENGTH);
		}

		// use the makeNameUnique function to append a digit after keywords
		if (KEYWORDS.contains(name) || KEYWORDS.contains(name.toUpperCase()))
		{
			name = makeNameUnique(name, Sets.newHashSet());
		}

		return name;
	}

	/**
	 * Recursively adds a count at the end of a name until it is unique within its scope. Assumes that the names passed
	 * are no longer than the max length.
	 */
	public String makeNameUnique(String name, int index, HashSet<String> scope)
	{
		int indexLength = String.valueOf(index).length();
		String newName;
		if ((name.length() + indexLength) <= MAX_NAME_LENGTH)
		{
			newName = name + index;
		}
		else
		{
			// adding numbers will exceed the max length limit, so cut some stuff off
			newName = name.substring(0, name.length() - indexLength);
			newName = newName + index;
		}

		if (scope.contains(newName))
		{
			return makeNameUnique(name, ++index, scope);
		}

		return newName;
	}

	public String makeNameUnique(String name, HashSet<String> scope)
	{
		return makeNameUnique(name, 1, scope);
	}

	public static final int MAX_NAME_LENGTH = 30;

	// https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
	// Case sensitive
	public static final Set<String> JAVA_KEYWORDS = Sets.newHashSet("abstract", "continue", "for", "new", "switch",
			"assert", "default", "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break",
			"double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
			"instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
			"interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
			"native", "super", "while");

	// http://dev.mysql.com/doc/mysqld-version-reference/en/mysqld-version-reference-reservedwords-5-6.html
	// Version: 5.6
	// Case insensitive
	public static final Set<String> MYSQL_KEYWORDS = Sets.newHashSet("ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE",
			"AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOTH", "BY", "CALL",
			"CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION", "CONSTRAINT",
			"CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
			"CURRENT_USER", "CURSOR", "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE",
			"DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DESC", "DESCRIBE",
			"DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE", "ELSEIF",
			"ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN", "FALSE", "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR",
			"FORCE", "FOREIGN", "FROM", "FULLTEXT", "GENERAL", "GET", "GRANT", "GROUP", "HAVING", "HIGH_PRIORITY",
			"HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE", "IGNORE_SERVER_IDS", "IN", "INDEX",
			"INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8",
			"INTEGER", "INTERVAL", "INTO", "IO_AFTER_GTIDS", "IO_BEFORE_GTIDS", "IS", "ITERATE", "JOIN", "KEY", "KEYS",
			"KILL", "LEADING", "LEAVE", "LEFT", "LIKE", "LIMIT", "LINEAR", "LINES", "LOAD", "LOCALTIME",
			"LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY", "MASTER_BIND",
			"MASTER_HEARTBEAT_PERIOD", "MASTER_SSL_VERIFY_SERVER_CERT", "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT",
			"MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL", "NOT",
			"NO_WRITE_TO_BINLOG", "NULL", "NUMERIC", "ON", "ONE_SHOT", "OPTIMIZE", "OPTION", "OPTIONALLY", "OR",
			"ORDER", "OUT", "OUTER", "OUTFILE", "PARTITION", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE", "RANGE",
			"READ", "READS", "READ_WRITE", "REAL", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT", "REPLACE",
			"REQUIRE", "RESIGNAL", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA", "SCHEMAS",
			"SECOND_MICROSECOND", "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SIGNAL", "SLOW", "SMALLINT",
			"SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_AFTER_GTIDS",
			"SQL_BEFORE_GTIDS", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING",
			"STRAIGHT_JOIN", "TABLE", "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING",
			"TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING",
			"UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING",
			"WHEN", "WHERE", "WHILE", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL");

	// http://www.w3schools.com/js/js_reserved.asp
	// Case sensitive
	public static final Set<String> JAVASCRIPT_KEYWORDS = Sets.newHashSet("abstract", "arguments", "boolean", "break",
			"byte", "case", "catch", "char", "class", "const", "continue", "debugger", "default", "delete", "do",
			"double", "else", "enum", "eval", "export", "extends", "false", "final", "finally", "float", "for",
			"function", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface", "let", "long",
			"native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "super",
			"switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void",
			"volatile", "while", "with", "yield");

	// Case sensitive
	public static final Set<String> MOLGENIS_KEYWORDS = Sets.newHashSet("login", "logout", "csv", "entities",
			"attributes", "base");

	// TODO: REMOVE!!!!
	public static final Set<String> TEST = Sets.newHashSet("molgenis", "thispackagenameiswaytoolongtob", "Owned",
			"tommy", "ontologyTermSynonym", "chromosome6_survey1", "Abnormality_of_body_height", "ontologyIRI",
			"Ontology", "umbilicalcord");

	public static Set<String> KEYWORDS = Sets.newHashSet();
	static
	{
		KEYWORDS.addAll(JAVA_KEYWORDS);
		KEYWORDS.addAll(JAVASCRIPT_KEYWORDS);
		KEYWORDS.addAll(MOLGENIS_KEYWORDS);
		KEYWORDS.addAll(MYSQL_KEYWORDS);
		// KEYWORDS.addAll(TEST);
	}
}
