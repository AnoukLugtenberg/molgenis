package org.molgenis.data.mapper.algorithmgenerator;

import java.util.Arrays;

import org.mockito.Mockito;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.DataService;
import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class CategoryAlgorithmGeneratorImplTest
{

	CategoryAlgorithmGeneratorImpl categoryAlgorithmGeneratorImpl;

	DefaultAttributeMetaData targetAttributeMetaData;

	DefaultAttributeMetaData sourceAttributeMetaData;

	@BeforeMethod
	public void init()
	{
		DataService dataService = Mockito.mock(DataService.class);
		categoryAlgorithmGeneratorImpl = new CategoryAlgorithmGeneratorImpl(dataService);

		DefaultEntityMetaData targetRefEntityMetaData = new DefaultEntityMetaData("POTATO_REF");
		DefaultAttributeMetaData targetCodeAttributeMetaData = new DefaultAttributeMetaData("code", FieldTypeEnum.INT);
		targetCodeAttributeMetaData.setIdAttribute(true);
		DefaultAttributeMetaData targetLabelAttributeMetaData = new DefaultAttributeMetaData("label",
				FieldTypeEnum.STRING);
		targetLabelAttributeMetaData.setLabelAttribute(true);
		targetRefEntityMetaData.addAttributeMetaData(targetCodeAttributeMetaData);
		targetRefEntityMetaData.addAttributeMetaData(targetLabelAttributeMetaData);

		targetAttributeMetaData = new DefaultAttributeMetaData("Current Consumption Frequency of Potatoes",
				FieldTypeEnum.CATEGORICAL);
		targetAttributeMetaData.setRefEntity(targetRefEntityMetaData);

		MapEntity targetEntity1 = new MapEntity(ImmutableMap.of("code", 1, "label", "Almost daily + daily"));
		MapEntity targetEntity2 = new MapEntity(ImmutableMap.of("code", 2, "label", "Several times a week"));
		MapEntity targetEntity3 = new MapEntity(ImmutableMap.of("code", 3, "label", "About once a week"));
		MapEntity targetEntity4 = new MapEntity(ImmutableMap.of("code", 4, "label", "Never + fewer than once a week"));
		MapEntity targetEntity5 = new MapEntity(ImmutableMap.of("code", 9, "label", "missing"));

		Mockito.when(dataService.findAll(targetRefEntityMetaData.getName())).thenReturn(
				Arrays.asList(targetEntity1, targetEntity2, targetEntity3, targetEntity4, targetEntity5));

		DefaultEntityMetaData sourceRefEntityMetaData = new DefaultEntityMetaData("LifeLines_POTATO_REF");

		DefaultAttributeMetaData sourceCodeAttributeMetaData = new DefaultAttributeMetaData("code", FieldTypeEnum.INT);
		sourceCodeAttributeMetaData.setIdAttribute(true);
		DefaultAttributeMetaData sourceLabelAttributeMetaData = new DefaultAttributeMetaData("label",
				FieldTypeEnum.STRING);
		sourceLabelAttributeMetaData.setLabelAttribute(true);
		sourceRefEntityMetaData.addAttributeMetaData(sourceCodeAttributeMetaData);
		sourceRefEntityMetaData.addAttributeMetaData(sourceLabelAttributeMetaData);

		sourceAttributeMetaData = new DefaultAttributeMetaData(
				"How often did you eat boiled or mashed potatoes (also in stew) in the past month? Baked potatoes are asked later",
				FieldTypeEnum.CATEGORICAL);
		sourceAttributeMetaData.setRefEntity(sourceRefEntityMetaData);

		MapEntity sourceEntity1 = new MapEntity(ImmutableMap.of("code", 1, "label", "Not this month"));
		MapEntity sourceEntity2 = new MapEntity(ImmutableMap.of("code", 2, "label", "1 day per month"));
		MapEntity sourceEntity3 = new MapEntity(ImmutableMap.of("code", 3, "label", "2-3 days per month"));
		MapEntity sourceEntity4 = new MapEntity(ImmutableMap.of("code", 4, "label", "1 day per week"));
		MapEntity sourceEntity5 = new MapEntity(ImmutableMap.of("code", 5, "label", "2-3 days per week"));
		MapEntity sourceEntity6 = new MapEntity(ImmutableMap.of("code", 6, "label", "4-5 days per week"));
		MapEntity sourceEntity7 = new MapEntity(ImmutableMap.of("code", 7, "label", "6-7 days per week"));

		Mockito.when(dataService.findAll(sourceRefEntityMetaData.getName())).thenReturn(
				Arrays.asList(sourceEntity1, sourceEntity2, sourceEntity3, sourceEntity4, sourceEntity5, sourceEntity6,
						sourceEntity7));
	}

	@Test
	public void generate()
	{
		String generate = categoryAlgorithmGeneratorImpl.generate(targetAttributeMetaData, sourceAttributeMetaData);

		System.out.println(generate);
	}
}
