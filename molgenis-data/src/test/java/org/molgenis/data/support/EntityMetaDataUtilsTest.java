package org.molgenis.data.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.Package;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

public class EntityMetaDataUtilsTest
{
	@Test
	public void getAttributeNames()
	{
		AttributeMetaData attr0 = when(mock(AttributeMetaData.class).getName()).thenReturn("attr0").getMock();
		AttributeMetaData attr1 = when(mock(AttributeMetaData.class).getName()).thenReturn("attr1").getMock();
		assertEquals(Lists.newArrayList(EntityMetaDataUtils.getAttributeNames(Arrays.asList(attr0, attr1))),
				Arrays.asList("attr0", "attr1"));
	}

	@Test
	public void buildFullName()
	{
//		assertEquals(EntityMetaDataUtils.buildFullName(Package.defaultPackage, "simpleName"), "simpleName"); // FIXME
		
		assertEquals(EntityMetaDataUtils.buildFullName(null, "simpleName"), "simpleName");

		Package package_1 = new Package("base");
		assertEquals(EntityMetaDataUtils.buildFullName(package_1, "simpleName"), "simpleName");

		Package package_2 = new Package("my_first_package");
		assertEquals(EntityMetaDataUtils.buildFullName(package_2, "simpleName"), "my_first_package_simpleName");
	}
}
