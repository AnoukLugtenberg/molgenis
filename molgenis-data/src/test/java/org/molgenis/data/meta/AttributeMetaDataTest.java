package org.molgenis.data.meta;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(classes = AttributeMetaDataTest.Config.class)
public class AttributeMetaDataTest extends AbstractTestNGSpringContextTests
{
	@Configuration
	public static class Config {
		@Bean
		public AttributeMetaDataFactory attributeMetaDataFactory() {
			return new AttributeMetaDataFactory();
		}

		@Bean
		public EntityMetaDataMetaData entityMetaDataMetaData() {
			return new EntityMetaDataMetaData();
		}

		@Bean
		public AttributeMetaDataMetaData attrMetaDataMetaData() {
			return new AttributeMetaDataMetaData();
		}

		@Bean
		public PackageMetaData packageMetaData() {
			return new PackageMetaData();
		}

		@Bean
		public TagMetaData tagMetaData() {
			return new TagMetaData();
		}
	}

	@Autowired
	private AttributeMetaDataMetaData attrMetaDataMetaData;

	@Test
	public void create() {
		new AttributeMetaData(attrMetaDataMetaData);
	}

	@Test
	public void AttributeMetaDataAttributeMetaData()
	{
		AttributeMetaData attr = new AttributeMetaData("attribute");
		attr.setAggregatable(true);
		attr.setAuto(true);
		attr.setDataType(MolgenisFieldTypes.INT);
		attr.setDefaultValue(null);
		attr.setDescription("description");
		attr.setLabel("label");
		attr.setNillable(true);
		attr.setRange(new Range(-1L, 1L));
		attr.setReadOnly(true);
		attr.setUnique(true);
		attr.setVisible(true);
		attr.setAttributeParts(emptyList());
		attr.setTags(emptyList());
		assertEquals(AttributeMetaData.newInstance(attr), attr);
	}

	@Test
	public void AttributeMetaDataUnknownLabelUseName()
	{
		assertEquals(new AttributeMetaData("attribute").getLabel(), "attribute");
	}

	@Test
	public void getAttributePart()
	{
		AttributeMetaData attr = new AttributeMetaData("attribute");
		String attrName = "Attr";
		assertNull(attr.getAttributePart(attrName));
		AttributeMetaData attrPart = when(mock(AttributeMetaData.class).getName()).thenReturn(attrName).getMock();
		attr.addAttributePart(attrPart);
		assertEquals(attrPart, attr.getAttributePart(attrName));
		assertEquals(attrPart, attr.getAttributePart(attrName.toLowerCase())); // case insensitive
	}
}
