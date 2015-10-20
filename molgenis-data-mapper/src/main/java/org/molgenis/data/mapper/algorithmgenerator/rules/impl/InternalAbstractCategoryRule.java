package org.molgenis.data.mapper.algorithmgenerator.rules.impl;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.mapper.algorithmgenerator.bean.Category;
import org.molgenis.data.mapper.algorithmgenerator.rules.CategoryRule;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

public abstract class InternalAbstractCategoryRule implements CategoryRule
{
	private static final Splitter TERM_SPLITTER = Splitter.onPattern("\\s+");
	private static final String ILLEGAL_CHARS_REGEX = "[^a-zA-Z0-9]";
	private final Set<String> words;

	public InternalAbstractCategoryRule(Set<String> words)
	{
		this.words = requireNonNull(words);
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
		return Sets.newHashSet(TERM_SPLITTER.split(label.toLowerCase())).stream().map(this::removeIllegalChars)
				.collect(Collectors.toSet());
	}

	protected String removeIllegalChars(String string)
	{
		return string.replaceAll(ILLEGAL_CHARS_REGEX, StringUtils.EMPTY);
	}
}
