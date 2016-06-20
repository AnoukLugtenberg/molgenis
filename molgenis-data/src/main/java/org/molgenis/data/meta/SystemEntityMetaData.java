package org.molgenis.data.meta;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.Package.PACKAGE_SEPARATOR;
import static org.molgenis.data.meta.RootSystemPackage.PACKAGE_SYSTEM;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all system entity meta data.
 */
public abstract class SystemEntityMetaData extends EntityMetaData
{
	private AttributeMetaDataFactory attributeMetaDataFactory;

	private final String entityName;
	private final String systemPackageName;

	/**
	 * Construct entity meta data for an entity with the given name stored in the system package.
	 *
	 * @param entityName entity name
	 */
	public SystemEntityMetaData(String entityName)
	{
		this(entityName, PACKAGE_SYSTEM);
	}

	/**
	 * Construct entity meta data for an entity with the given name stored in a system package with the given package name.
	 *
	 * @param entityName        entity name
	 * @param systemPackageName system package name
	 */
	public SystemEntityMetaData(String entityName, String systemPackageName)
	{
		this.entityName = requireNonNull(entityName);
		this.systemPackageName = requireNonNull(systemPackageName);
		if (!systemPackageName.startsWith(PACKAGE_SYSTEM))
		{
			throw new IllegalArgumentException(
					format("Entity [%s] must be located in package [%s] instead of [%s]", entityName, PACKAGE_SYSTEM,
							systemPackageName));
		}
	}

	public void bootstrap(EntityMetaDataMetaData entityMetaDataMetaData)
	{
		super.init(entityMetaDataMetaData);
		setName(systemPackageName + PACKAGE_SEPARATOR + entityName);
		setSimpleName(entityName);
		init();
	}

	/**
	 * Initialize system entity meta data, e.g. adding attributes, setting package
	 */
	protected abstract void init();

	@Override
	public String getName()
	{
		return systemPackageName + PACKAGE_SEPARATOR + entityName;
	}

	@Override
	public String getSimpleName()
	{
		return entityName;
	}

	public AttributeMetaData addAttribute(String attrName, AttributeRole... attrTypes)
	{
		return addAttribute(attrName, null, attrTypes);
	}

	public AttributeMetaData addAttribute(String attrName, AttributeMetaData parentAttr, AttributeRole... attrTypes)
	{
		AttributeMetaData attr = attributeMetaDataFactory.create();
		attr.setName(attrName);
		if (parentAttr != null)
		{
			parentAttr.addAttributePart(attr);
			// FIXME assign roles, see super.addAttribute(AttributeMetaData attr, AttributeRole... attrTypes)
		}
		else
		{
			addAttribute(attr, attrTypes);
		}
		return attr;
	}

	// setter injection instead of constructor injection to avoid unresolvable circular dependencies
	@Autowired
	public void setAttributeMetaDataFactory(AttributeMetaDataFactory attributeMetaDataFactory)
	{
		this.attributeMetaDataFactory = requireNonNull(attributeMetaDataFactory);
	}
}
