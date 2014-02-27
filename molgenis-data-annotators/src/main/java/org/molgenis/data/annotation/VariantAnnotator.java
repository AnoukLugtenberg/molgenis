package org.molgenis.data.annotation;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;

public abstract class VariantAnnotator extends LocusAnnotator
{
	private static final String REFERENCE = "ref";
	private static final String ALTERNATIVE = "alt";

	@Override
	public EntityMetaData getInputMetaData()
	{
		DefaultEntityMetaData metadata = new DefaultEntityMetaData(this.getClass().getName());

		metadata.addAttributeMetaData(new DefaultAttributeMetaData(CHROMOSOME, MolgenisFieldTypes.FieldTypeEnum.STRING));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(POSITION, MolgenisFieldTypes.FieldTypeEnum.LONG));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(REFERENCE, MolgenisFieldTypes.FieldTypeEnum.STRING));
		metadata.addAttributeMetaData(new DefaultAttributeMetaData(ALTERNATIVE, MolgenisFieldTypes.FieldTypeEnum.STRING));

		return metadata;
	}
}
