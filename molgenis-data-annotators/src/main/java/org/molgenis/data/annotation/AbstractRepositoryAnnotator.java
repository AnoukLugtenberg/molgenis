package org.molgenis.data.annotation;

import org.molgenis.data.Entity;
import org.molgenis.data.annotation.entity.AnnotatorInfo;
import org.molgenis.data.meta.model.AttributeMetaData;
import org.molgenis.data.meta.model.EntityMetaData;

import java.util.Iterator;

import static org.molgenis.MolgenisFieldTypes.AttributeType.STRING;
import static org.molgenis.MolgenisFieldTypes.AttributeType.TEXT;

public abstract class AbstractRepositoryAnnotator implements RepositoryAnnotator
{
	@Override
	public String canAnnotate(EntityMetaData repoMetaData)
	{
		Iterable<AttributeMetaData> annotatorAttributes = getRequiredAttributes();
		for (AttributeMetaData annotatorAttribute : annotatorAttributes)
		{
			// one of the needed attributes not present? we can not annotate
			if (repoMetaData.getAttribute(annotatorAttribute.getName()) == null)
			{
				return "missing required attribute";
			}

			// one of the needed attributes not of the correct type? we can not annotate
			if (repoMetaData.getAttribute(annotatorAttribute.getName()).getDataType() != annotatorAttribute
					.getDataType())
			{
				// allow type string when required attribute is text (for backward compatibility)
				if (!(repoMetaData.getAttribute(annotatorAttribute.getName()).getDataType() == STRING
						&& annotatorAttribute.getDataType() == TEXT))
				{
					return "a required attribute has the wrong datatype";
				}
			}

			// Are the runtime property files not available, or is a webservice down? we can not annotate
			if (!annotationDataExists())
			{
				return "annotation datasource unreachable";
			}
		}

		return "true";
	}

	@Override
	public Iterator<Entity> annotate(final Iterator<Entity> sourceIterable)
	{
		return this.annotate(new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return sourceIterable;
			}
		});
	}

	@Override
	public String getFullName()
	{
		return RepositoryAnnotator.ANNOTATOR_PREFIX + getSimpleName();
	}

	@Override
	public String getDescription()
	{
		String desc = "TODO";
		AnnotatorInfo annotatorInfo = getInfo();
		if (annotatorInfo != null) desc = annotatorInfo.getDescription();
		return desc;
	}

}
