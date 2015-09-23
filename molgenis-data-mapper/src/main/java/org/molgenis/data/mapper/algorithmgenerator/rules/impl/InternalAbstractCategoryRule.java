package org.molgenis.data.mapper.algorithmgenerator.rules.impl;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.mapper.algorithmgenerator.bean.Category;
import org.molgenis.data.mapper.algorithmgenerator.rules.CategoryRule;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

public abstract class InternalAbstractCategoryRule implements CategoryRule
{
	private final Splitter termSplitter = Splitter.onPattern("\\s+");
	private final Set<String> words;

	public InternalAbstractCategoryRule(Set<String> words)
	{
		this.words = Objects.requireNonNull(words);
	}

	@Override
	public boolean isRuleApplied(Category targetCategory, Category sourceCategory)
	{
		String targetLabel = targetCategory.getLabel();
		String sourceLabel = sourceCategory.getLabel();

		return labelContainsWords(sourceLabel) && labelContainsWords(targetLabel);
	}

	protected boolean labelContainsWords(String label)
	{
		if (StringUtils.isNotBlank(label))
		{
			Set<String> tokens = split(label);
			return words.stream().anyMatch(word -> tokens.containsAll(split(word)));
		}
		return false;
	}

	protected Set<String> split(String label)
	{
		return Sets.newHashSet(termSplitter.split(label.toLowerCase()));
	}
}
