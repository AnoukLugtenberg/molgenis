package org.molgenis.oneclickimporter.service.Impl;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityManager;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.DefaultPackage;
import org.molgenis.data.meta.MetaDataService;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.AttributeFactory;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.meta.model.EntityTypeFactory;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.oneclickimporter.model.Column;
import org.molgenis.oneclickimporter.model.DataCollection;
import org.molgenis.oneclickimporter.service.EntityService;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.AttributeType.*;

@Component
public class EntityServiceImpl implements EntityService
{
	public static final String ID_ATTR_NAME = "id_";

	private final DefaultPackage defaultPackage;

	private final EntityTypeFactory entityTypeFactory;

	private final AttributeFactory attributeFactory;

	private final IdGenerator idGenerator;

	private final DataService dataService;

	private final EntityManager entityManager;

	public EntityServiceImpl(DefaultPackage defaultPackage, EntityTypeFactory entityTypeFactory,
			AttributeFactory attributeFactory, IdGenerator idGenerator, DataService dataService,
			EntityManager entityManager)
	{
		this.defaultPackage = requireNonNull(defaultPackage);
		this.entityTypeFactory = requireNonNull(entityTypeFactory);
		this.attributeFactory = requireNonNull(attributeFactory);
		this.idGenerator = requireNonNull(idGenerator);
		this.dataService = requireNonNull(dataService);
		this.entityManager = requireNonNull(entityManager);
	}

	@Override
	public EntityType createEntity(DataCollection dataCollection)
	{
		// Create a dataTable
		EntityType entityType = entityTypeFactory.create();
		entityType.setPackage(defaultPackage);
		entityType.setId(idGenerator.generateId());
		entityType.setLabel(dataCollection.getName());

		// Add a auto id column
		Attribute idAttribute = attributeFactory.create()
												.setName(ID_ATTR_NAME)
												.setVisible(Boolean.FALSE)
												.setAuto(Boolean.TRUE)
												.setIdAttribute(Boolean.TRUE);
		entityType.addAttribute(idAttribute, EntityType.AttributeRole.ROLE_ID);

		// Store the dataTable
		MetaDataService meta = dataService.getMeta();
		meta.addEntityType(entityType);

		// Add the columns the the dataTable
		for (Column column : dataCollection.getColumns())
		{

			Attribute attribute = attributeFactory.create();
			attribute.setName(column.getName());

			attribute.setDataType(guessAttributeType(column.getDataValues()));

			entityType.addAttribute(attribute);

			meta.updateEntityType(entityType);
		}

		// Fill the dataTable with data
		// All columns have a equal number of rows
		int numberOfRows = dataCollection.getColumns().get(0).getDataValues().size();
		for (int columnIndex = 0; columnIndex < numberOfRows; columnIndex++)
		{

			Entity row = entityManager.create(entityType, EntityManager.CreationMode.NO_POPULATE);
			row.setIdValue(idGenerator.generateId());

			for (Column column : dataCollection.getColumns())
			{
				row.set(column.getName(), column.getDataValues().get(columnIndex));
			}

			dataService.add(entityType.getId(), row);
		}

		return entityType;
	}

	private AttributeType guessAttributeType(List<Object> dataValues)
	{
		Object value = dataValues.get(0);
		if (value instanceof Integer)
		{
			return INT;
		}
		else if (value instanceof Double || value instanceof Float)
		{
			return DECIMAL;
		}
		else if (value instanceof Long)
		{
			return LONG;
		}
		else if (value instanceof Boolean)
		{
			return BOOL;
		}
		else
		{
			return STRING;
		}
	}
}
