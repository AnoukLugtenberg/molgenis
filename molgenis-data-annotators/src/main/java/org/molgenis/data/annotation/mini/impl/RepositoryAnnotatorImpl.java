package org.molgenis.data.annotation.mini.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.annotation.AbstractRepositoryAnnotator;
import org.molgenis.data.annotation.mini.AnnotatorInfo;
import org.molgenis.data.annotation.mini.EntityAnnotator;

public class RepositoryAnnotatorImpl extends AbstractRepositoryAnnotator
{
	private EntityAnnotator entityAnnotator;
	private EntityMetaData metaData;

	public RepositoryAnnotatorImpl(EntityAnnotator entityAnnotator, EntityMetaData metaData)
	{
		this.entityAnnotator = entityAnnotator;
		this.metaData = metaData;
	}

	@Override
	public List<AttributeMetaData> getOutputMetaData()
	{
		List<AttributeMetaData> result = new ArrayList<>();
		result.add(entityAnnotator.getAnnotationAttributeMetaData());
		return result;
	}

	@Override
	public List<AttributeMetaData> getInputMetaData()
	{
		return entityAnnotator.getRequiredAttributes();
	}

	@Override
	public String getSimpleName()
	{
		return entityAnnotator.getInfo().getCode();
	}

	@Override
	protected boolean annotationDataExists()
	{
		return entityAnnotator.sourceExists();
	}

	@Override
	public List<Entity> annotateEntity(Entity entity) throws IOException, InterruptedException
	{
		return Lists.newArrayList(entityAnnotator.annotateEntity(entity));
	}

	@Override
	public AnnotatorInfo getInfo()
	{
		return entityAnnotator.getInfo();
	}

}
