package org.molgenis.data.reindex.meta;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.Package.PACKAGE_SEPARATOR;
import static org.molgenis.data.reindex.meta.IndexPackage.PACKAGE_INDEX;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This entity is used to group the reindex actions.
 */
@Component
public class ReindexActionJobMetaData extends SystemEntityMetaDataImpl
{
	public static final String SIMPLE_NAME = "ReindexActionJob";
	public static final String REINDEX_ACTION_JOB = PACKAGE_INDEX + PACKAGE_SEPARATOR + SIMPLE_NAME;

	/**
	 * Example: Transaction id can be used to group all actions into one transaction.
	 */
	public static final String ID = "id";

	/**
	 * The amount of actions that are grouped together. It is being used to determine the order of the action.
	 */
	public static final String COUNT = "count";

	private final IndexPackage indexPackage;

	@Autowired
	public ReindexActionJobMetaData(IndexPackage indexPackage)
	{
		super(SIMPLE_NAME, PACKAGE_INDEX);
		this.indexPackage = requireNonNull(indexPackage);
	}

	@Override
	public void init()
	{
		setPackage(indexPackage);

		setDescription("This entity is used to group the reindex actions.");
		addAttribute(ID, ROLE_ID);
		addAttribute(COUNT).setDataType(MolgenisFieldTypes.INT).setNillable(false);
	}
}
