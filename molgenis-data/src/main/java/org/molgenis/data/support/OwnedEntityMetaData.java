package org.molgenis.data.support;

import static org.molgenis.MolgenisFieldTypes.STRING;

import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.springframework.stereotype.Component;

/**
 * Defines an abstract EntityMetaData for entities that have an 'owner'.
 * <p>
 * These entities can only be viewed/updated/deleted by it's creator.
 * <p>
 * Defines one attribute 'ownerUsername', that is the username of the owner. You can extend this EntityMetaData to
 * inherit this behavior.
 */
@Component
public class OwnedEntityMetaData extends SystemEntityMetaDataImpl
{
	public static final String ENTITY_NAME = "Owned";
	public static final String ATTR_OWNER_USERNAME = "ownerUsername";

	@Override
	public void init()
	{
		setName(ENTITY_NAME);
		setAbstract(true);
		addAttribute(ATTR_OWNER_USERNAME).setDataType(STRING).setVisible(false);
	}
}
