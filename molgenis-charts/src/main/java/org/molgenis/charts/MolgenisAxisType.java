package org.molgenis.charts;

import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;

public enum MolgenisAxisType
{
	LINEAR, LOGARITHMIC, DATETIME, CATEGORY;

	private MolgenisAxisType()
	{
	}

	/**
	 * Get a Molgenis axis type based on an attributeJavaType
	 * 
	 * @param attributeJavaType
	 *            Class<?>
	 * @return MolgenisAxisType
	 */
	public static MolgenisAxisType getType(FieldTypeEnum attributeFieldTypeEnum)
	{
		if (FieldTypeEnum.DECIMAL.equals(attributeFieldTypeEnum) || FieldTypeEnum.INT.equals(attributeFieldTypeEnum)
				|| FieldTypeEnum.LONG.equals(attributeFieldTypeEnum))
		{
			return MolgenisAxisType.LINEAR;
		}
		else if (FieldTypeEnum.DATE.equals(attributeFieldTypeEnum))
		{
			return MolgenisAxisType.DATETIME;
		}
		else if (FieldTypeEnum.DATE_TIME.equals(attributeFieldTypeEnum))
		{
			return MolgenisAxisType.DATETIME;
		}
		else
		{
			return MolgenisAxisType.CATEGORY;
		}
	}
}
