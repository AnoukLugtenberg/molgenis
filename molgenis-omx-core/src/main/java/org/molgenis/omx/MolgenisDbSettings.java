package org.molgenis.omx;

import org.apache.log4j.Logger;
import org.molgenis.data.DataService;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.framework.server.MolgenisSettings;
import org.molgenis.omx.core.RuntimeProperty;
import org.molgenis.security.runas.RunAsSystem;
import org.springframework.beans.factory.annotation.Autowired;

public class MolgenisDbSettings implements MolgenisSettings
{
	private static final Logger logger = Logger.getLogger(MolgenisDbSettings.class);

	private final DataService dataService;

	@Autowired
	public MolgenisDbSettings(DataService dataService)
	{
		if (dataService == null) throw new IllegalArgumentException("DataService is null");
		this.dataService = dataService;
	}

	@Override
	@RunAsSystem
	public String getProperty(String key)
	{
		return getProperty(key, null);
	}

	@Override
	@RunAsSystem
	public String getProperty(String key, String defaultValue)
	{
		QueryRule propertyRule = new QueryRule(RuntimeProperty.IDENTIFIER, Operator.EQUALS,
				RuntimeProperty.class.getSimpleName() + '_' + key);

		RuntimeProperty property;
		try
		{
			property = dataService.findOne(RuntimeProperty.ENTITY_NAME, propertyRule);
			// property = DataSecurityUtils.findOneAsSystem(dataService, RuntimeProperty.ENTITY_NAME, new QueryImpl(
			// propertyRule));
		}
		catch (MolgenisDataException e)
		{
			logger.warn(e);
			return defaultValue;
		}

		if (property == null)
		{
			logger.warn(RuntimeProperty.class.getSimpleName() + " '" + key + "' is null");
			return defaultValue;
		}

		return property.getValue();
	}

	@Override
	public void setProperty(String key, String value)
	{
		RuntimeProperty property = new RuntimeProperty();
		property.setIdentifier(RuntimeProperty.class.getSimpleName() + '_' + key);
		property.setName(key);
		property.setValue(value);

		dataService.add(RuntimeProperty.ENTITY_NAME, property);
	}

	@Override
	public Boolean getBooleanProperty(String key)
	{
		String value = getProperty(key);
		if (value == null)
		{
			return null;
		}

		return Boolean.valueOf(value);
	}

	@Override
	public boolean getBooleanProperty(String key, boolean defaultValue)
	{
		Boolean value = getBooleanProperty(key);
		if (value == null)
		{
			return defaultValue;
		}

		return value;
	}
	
	@Override
	public boolean updateProperty(String key, String content) 
	{
		if (null == content)
		{
			throw new IllegalArgumentException("content is null");
		}
		
		QueryRule propertyRule = new QueryRule(RuntimeProperty.IDENTIFIER, Operator.EQUALS,
				RuntimeProperty.class.getSimpleName() + '_' + key);
		List<RuntimeProperty> properties;
		
		try
		{
			properties = unsecuredDatabase.find(RuntimeProperty.class, propertyRule);
			if (null != properties && !properties.isEmpty() && properties.size() == 1) {
				RuntimeProperty property = properties.get(0);
				property.setValue(content);
				int result = unsecuredDatabase.update(property);
				logger.info("result: " + result);
				return true;
			}
		}
		catch (DatabaseException e)
		{
			logger.warn(e);
		}
		
		return false;
	}
	
	@Override
	public boolean propertyExists(String key) 
	{		
		QueryRule propertyRule = new QueryRule(RuntimeProperty.IDENTIFIER, Operator.EQUALS,
				RuntimeProperty.class.getSimpleName() + '_' + key);
		int count;
		
		try
		{
			count = unsecuredDatabase.count(RuntimeProperty.class, propertyRule);
			if (count > 0) {
				return true;
			}
		}
		catch (DatabaseException e)
		{
			logger.warn(e);
		}
		
		return false;
	}
}
