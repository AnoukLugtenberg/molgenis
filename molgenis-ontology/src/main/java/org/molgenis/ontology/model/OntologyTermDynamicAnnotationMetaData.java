package org.molgenis.ontology.model;

import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;

public class OntologyTermDynamicAnnotationMetaData
{
	public final static String ID = "id";
	public final static String NAME = "name";
	public final static String VALUE = "value";
	public final static String LABEL = "label";
	public final static String ENTITY_NAME = "OntologyTermDynamicAnnotation";

	public static EntityMetaData getEntityMetaData()
	{
		DefaultEntityMetaData entityMetaData = new DefaultEntityMetaData(ENTITY_NAME);

		DefaultAttributeMetaData idAttr = new DefaultAttributeMetaData(ID);
		idAttr.setIdAttribute(true);
		idAttr.setNillable(false);
		entityMetaData.addAttributeMetaData(idAttr);

		DefaultAttributeMetaData nameAttr = new DefaultAttributeMetaData(NAME, FieldTypeEnum.STRING);
		nameAttr.setNillable(false);
		entityMetaData.addAttributeMetaData(nameAttr);

		DefaultAttributeMetaData valueAttr = new DefaultAttributeMetaData(VALUE, FieldTypeEnum.STRING);
		valueAttr.setNillable(false);
		entityMetaData.addAttributeMetaData(valueAttr);

		DefaultAttributeMetaData labelAttr = new DefaultAttributeMetaData(LABEL, FieldTypeEnum.STRING);
		labelAttr.setNillable(false);
		labelAttr.setLabelAttribute(true);
		entityMetaData.addAttributeMetaData(labelAttr);

		return entityMetaData;
	}
}