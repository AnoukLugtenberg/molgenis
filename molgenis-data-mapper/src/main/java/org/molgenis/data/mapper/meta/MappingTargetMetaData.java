package org.molgenis.data.mapper.meta;

import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.data.mapper.repository.impl.EntityMappingRepositoryImpl;
import org.molgenis.data.meta.EntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class MappingTargetMetaData extends EntityMetaData
{
	public static final String ENTITY_NAME = "MappingTarget";
	public static final String IDENTIFIER = "identifier";
	public static final String ENTITYMAPPINGS = "entityMappings";
	public static final String TARGET = "target";

	public MappingTargetMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(ENTITYMAPPINGS).setDataType(MREF).setRefEntity(EntityMappingRepositoryImpl.META_DATA);
		addAttribute(TARGET).setNillable(false);
	}
}
