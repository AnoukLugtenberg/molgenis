package org.molgenis.data.idcard.model;

import static org.molgenis.data.idcard.model.IdCardBiobankMetaData.ORGANIZATION_ID;

import org.molgenis.data.Entity;
import org.molgenis.data.meta.SystemEntity;

public class IdCardBiobank extends SystemEntity
{
	public IdCardBiobank(Entity entity)
	{
		super(entity);
	}

	public IdCardBiobank(IdCardBiobankMetaData idCardBiobankMetaData)
	{
		super(idCardBiobankMetaData);
	}

	public IdCardBiobank(Integer identifier, IdCardBiobankMetaData idCardBiobankMetaData)
	{
		super(idCardBiobankMetaData);
		set(ORGANIZATION_ID, identifier);
	}

	// FIXME add and use getters/setters
}
