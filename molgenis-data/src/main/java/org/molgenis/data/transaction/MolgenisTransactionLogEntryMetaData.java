package org.molgenis.data.transaction;

import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.Package.PACKAGE_SEPARATOR;
import static org.molgenis.data.meta.RootSystemPackage.PACKAGE_SYSTEM;

import java.util.ArrayList;
import java.util.List;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.molgenis.fieldtypes.EnumField;

public class MolgenisTransactionLogEntryMetaData extends SystemEntityMetaDataImpl
{
	public static final String SIMPLE_NAME = "MolgenisTransactionLogEntry";
	public static final String MOLGENIS_TRANSACTION_LOG_ENTRY = PACKAGE_SYSTEM + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String ID = "id";
	public static final String MOLGENIS_TRANSACTION_LOG = "molgenisTransactionLog";
	public static final String ENTITY = "entity";
	public static final String TYPE = "type";

	MolgenisTransactionLogEntryMetaData(MolgenisTransactionLogMetaData molgenisTransactionLogMetaData, String backend)
	{
		super(SIMPLE_NAME, PACKAGE_SYSTEM);
		setBackend(backend);
		addAttribute(ID, ROLE_ID).setAuto(true).setVisible(false);
		addAttribute(MOLGENIS_TRANSACTION_LOG).setDataType(MolgenisFieldTypes.XREF)
				.setRefEntity(molgenisTransactionLogMetaData);
		addAttribute(ENTITY).setNillable(false);
		addAttribute(TYPE).setDataType(new EnumField()).setEnumOptions(Type.getOptions()).setNillable(false);
	}

	@Override
	public void init()
	{
		// FIXME implement
	}

	public enum Type
	{
		ADD, UPDATE, DELETE;

		private static List<String> getOptions()
		{
			List<String> options = new ArrayList<String>();
			for (Type type : Type.values())
			{
				options.add(type.name());
			}

			return options;
		}
	};
}
