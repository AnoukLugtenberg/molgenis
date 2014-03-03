package org.molgenis.data.annotation.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DbnsfpVariantServiceAnnotatorTest
{
	private EntityMetaData metaDataCanAnnotate;
	private EntityMetaData metaDataCantAnnotate;
	private DbnsfpVariantServiceAnnotator annotator;
	private AttributeMetaData attributeMetaDataChrom;
	private AttributeMetaData attributeMetaDataPos;
	private AttributeMetaData attributeMetaDataRef;
	private AttributeMetaData attributeMetaDataAlt;
	private AttributeMetaData attributeMetaDataCantAnnotateFeature;
	private AttributeMetaData attributeMetaDataCantAnnotateChrom;
	private AttributeMetaData attributeMetaDataCantAnnotatePos;
	private AttributeMetaData attributeMetaDataCantAnnotateRef;
	private AttributeMetaData attributeMetaDataCantAnnotateAlt;
	private String annotatorOutput;
	private Entity entity;
	private ArrayList<Entity> input;

	@BeforeMethod
	public void beforeMethod()
	{

		annotator = new DbnsfpVariantServiceAnnotator();

		metaDataCanAnnotate = mock(EntityMetaData.class);

		attributeMetaDataChrom = mock(AttributeMetaData.class);
		attributeMetaDataPos = mock(AttributeMetaData.class);
		attributeMetaDataRef = mock(AttributeMetaData.class);
		attributeMetaDataAlt = mock(AttributeMetaData.class);

		when(attributeMetaDataChrom.getName()).thenReturn(DbnsfpVariantServiceAnnotator.CHROMOSOME);
		when(attributeMetaDataPos.getName()).thenReturn(DbnsfpVariantServiceAnnotator.POSITION);
		when(attributeMetaDataRef.getName()).thenReturn(DbnsfpVariantServiceAnnotator.REFERENCE);
		when(attributeMetaDataAlt.getName()).thenReturn(DbnsfpVariantServiceAnnotator.ALTERNATIVE);

		when(attributeMetaDataChrom.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));
		when(attributeMetaDataPos.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.LONG.toString().toLowerCase()));
		when(attributeMetaDataRef.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));
		when(attributeMetaDataAlt.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));

		when(metaDataCanAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.CHROMOSOME)).thenReturn(
				attributeMetaDataChrom);
		when(metaDataCanAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.POSITION)).thenReturn(attributeMetaDataPos);
		when(metaDataCanAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.REFERENCE))
				.thenReturn(attributeMetaDataRef);
		when(metaDataCanAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.ALTERNATIVE)).thenReturn(
				attributeMetaDataAlt);

		metaDataCantAnnotate = mock(EntityMetaData.class);

		attributeMetaDataCantAnnotateFeature = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotateFeature.getName()).thenReturn("otherID");
		when(attributeMetaDataCantAnnotateFeature.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));

		attributeMetaDataCantAnnotateChrom = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotateChrom.getName()).thenReturn(DbnsfpVariantServiceAnnotator.CHROMOSOME);
		when(attributeMetaDataCantAnnotateFeature.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.INT.toString().toLowerCase()));

		attributeMetaDataCantAnnotatePos = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotatePos.getName()).thenReturn(DbnsfpVariantServiceAnnotator.POSITION);
		when(attributeMetaDataCantAnnotatePos.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));

		attributeMetaDataCantAnnotateRef = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotateRef.getName()).thenReturn(DbnsfpVariantServiceAnnotator.REFERENCE);
		when(attributeMetaDataCantAnnotateRef.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.INT.toString().toLowerCase()));

		attributeMetaDataCantAnnotateAlt = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotateAlt.getName()).thenReturn(DbnsfpVariantServiceAnnotator.ALTERNATIVE);
		when(attributeMetaDataCantAnnotateAlt.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.INT.toString().toLowerCase()));

		when(metaDataCantAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.CHROMOSOME)).thenReturn(
				attributeMetaDataChrom);
		when(metaDataCantAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.POSITION)).thenReturn(
				attributeMetaDataCantAnnotatePos);
		when(metaDataCantAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.REFERENCE)).thenReturn(
				attributeMetaDataCantAnnotateRef);
		when(metaDataCantAnnotate.getAttribute(DbnsfpVariantServiceAnnotator.ALTERNATIVE)).thenReturn(
				attributeMetaDataCantAnnotateAlt);

		entity = mock(Entity.class);

		when(entity.getString(DbnsfpVariantServiceAnnotator.CHROMOSOME)).thenReturn("Y");
		when(entity.getLong(DbnsfpVariantServiceAnnotator.POSITION)).thenReturn(new Long(2655049));
		when(entity.getString(DbnsfpVariantServiceAnnotator.REFERENCE)).thenReturn("C");
		when(entity.getString(DbnsfpVariantServiceAnnotator.ALTERNATIVE)).thenReturn("A");

		input = new ArrayList<Entity>();
		input.add(entity);

		annotatorOutput = "Q	H	2715049	SRY	.	.	.	.	-	CAG";
	}

	@Test
	public void annotateTest()
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[4], "Q");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[5], "H");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[6], "2715049");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[7], "SRY");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[8], ".");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[9], ".");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[10], ".");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[11], ".");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[12], "-");
		resultMap.put(DbnsfpVariantServiceAnnotator.FEATURES[13], "CAG");

		Entity expectedEntity = new MapEntity(resultMap);

		expectedList.add(expectedEntity);

		Iterator<Entity> results = annotator.annotate(input.iterator());

		Entity resultEntity = results.next();

		for (int i = 4; i < 14; i++)
		{
			assertEquals(resultEntity.get(DbnsfpVariantServiceAnnotator.FEATURES[i]),
					expectedEntity.get(DbnsfpVariantServiceAnnotator.FEATURES[i]));
		}
	}

	@Test
	public void canAnnotateTrueTest()
	{
		assertEquals(annotator.canAnnotate(metaDataCanAnnotate), true);
	}

	@Test
	public void canAnnotateFalseTest()
	{
		assertEquals(annotator.canAnnotate(metaDataCantAnnotate), false);
	}
}
