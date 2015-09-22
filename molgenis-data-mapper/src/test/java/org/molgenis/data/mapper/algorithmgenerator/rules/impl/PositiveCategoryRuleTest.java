package org.molgenis.data.mapper.algorithmgenerator.rules.impl;

import org.molgenis.data.mapper.algorithmgenerator.bean.Category;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PositiveCategoryRuleTest
{

	PositiveCategoryRule rule = new PositiveCategoryRule();

	@Test
	public void isRuleApplied()
	{
		Assert.assertTrue(rule.isRuleApplied(Category.create(0, "has had stroke"), Category.create(1, "yes")));
		Assert.assertFalse(rule.isRuleApplied(Category.create(0, "has had stroke"), Category.create(1, "NO")));
	}

	@Test
	public void labelContainsPositiveWords()
	{
		Assert.assertFalse(rule.labelContainsPositiveWords("string NO NEVER contain the word!"));
		Assert.assertTrue(rule.labelContainsPositiveWords("string EVER contain the word!"));
		Assert.assertTrue(rule.labelContainsPositiveWords("string YES contain the word!"));
		Assert.assertTrue(rule.labelContainsPositiveWords("string HAS contain the word!"));
	}
}
