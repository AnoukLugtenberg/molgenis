package org.molgenis.ontology.core.meta;

import static org.molgenis.MolgenisFieldTypes.BOOL;
import static org.molgenis.MolgenisFieldTypes.TEXT;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.EntityMetaData.AttributeRole.ROLE_LABEL;

import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.EntityMetaDataImpl;
import org.molgenis.data.meta.SystemEntityMetaDataImpl;
import org.molgenis.ontology.core.model.OntologyPackage;
import org.springframework.stereotype.Component;

@Component
public class OntologyTermNodePathMetaData extends SystemEntityMetaDataImpl
{
	public final static String ID = "id";
	public final static String ONTOLOGY_TERM_NODE_PATH = "nodePath";
	public final static String ROOT = "root";
	public final static String SIMPLE_NAME = "OntologyTermNodePath";
	public final static String ENTITY_NAME = OntologyPackage.PACKAGE_NAME + "_" + SIMPLE_NAME;

	@Override
	public void init()
	{
		setName(SIMPLE_NAME);
		setPackage(OntologyPackage.getPackageInstance());
		addAttribute(ID, ROLE_ID).setVisible(false);
		addAttribute(ONTOLOGY_TERM_NODE_PATH, ROLE_LABEL).setDataType(TEXT).setNillable(false);
		addAttribute(ROOT).setDataType(BOOL).setNillable(false);
	}
}