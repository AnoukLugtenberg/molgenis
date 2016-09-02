package org.molgenis.data.elasticsearch.index.job;

import org.molgenis.data.AbstractSystemEntityFactory;
import org.molgenis.data.elasticsearch.index.job.IndexJobExecution;
import org.molgenis.data.elasticsearch.index.meta.IndexJobExecutionMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndexJobExecutionFactory
		extends AbstractSystemEntityFactory<IndexJobExecution, IndexJobExecutionMeta, String>
{
	@Autowired
	IndexJobExecutionFactory(IndexJobExecutionMeta indexJobExecutionMeta)
	{
		super(IndexJobExecution.class, indexJobExecutionMeta);
	}
}
