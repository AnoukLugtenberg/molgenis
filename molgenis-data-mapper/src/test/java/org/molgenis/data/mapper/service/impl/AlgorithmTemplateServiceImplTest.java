package org.molgenis.data.mapper.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.script.ScriptMetaData.SCRIPT;
import static org.molgenis.script.ScriptMetaData.TYPE;
import static org.molgenis.script.ScriptParameterMetaData.SCRIPT_PARAMETER;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.data.DataService;
import org.molgenis.data.Query;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.meta.EntityMetaDataImpl;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.script.Script;
import org.molgenis.script.ScriptMetaData;
import org.molgenis.script.ScriptParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = AlgorithmTemplateServiceImplTest.Config.class)
public class AlgorithmTemplateServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private AlgorithmTemplateServiceImpl algorithmTemplateServiceImpl;

	@Autowired
	private DataService dataService;

	private Script script0;
	private String param0Name = "param0", param1Name = "param1";

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		ScriptParameter param0 = null; // new ScriptParameter(dataService); // FIXME
		param0.setName(param0Name);

		ScriptParameter param1 = null; // new ScriptParameter(dataService); // FIXME
		param1.setName(param1Name);

		script0 = null; // FIXME new Script(dataService);
		script0.setName("name");
		script0.setContent(String.format("$('%s'),$('%s')", param0, param1));
		script0.set(ScriptMetaData.PARAMETERS, Arrays.asList(param0, param1));

		Query<Script> q = new QueryImpl<Script>().eq(TYPE, "type"); // FIXME
		when(dataService.findAll(SCRIPT, q, Script.class)).thenReturn(Stream.of(script0));
		when(dataService.findOneById(SCRIPT_PARAMETER, param0Name)).thenReturn(param0);
		when(dataService.findOneById(SCRIPT_PARAMETER, param1Name)).thenReturn(param1);
	}

	@Test
	public void find()
	{
		String sourceAttr0Name = "sourceAttr0";
		String sourceAttr1Name = "sourceAttr1";
		EntityMetaData sourceEntityMeta = new EntityMetaDataImpl("source");
		AttributeMetaData sourceAttr0 = sourceEntityMeta.addAttribute(sourceAttr0Name);
		AttributeMetaData sourceAttr1 = sourceEntityMeta.addAttribute(sourceAttr1Name);

		ExplainedQueryString sourceAttr0Explain = ExplainedQueryString.create("a", "b", param0Name, 1.0);
		ExplainedQueryString sourceAttr1Explain = ExplainedQueryString.create("a", "b", param1Name, 0.5);
		Map<AttributeMetaData, ExplainedAttributeMetaData> attrResults = new HashMap<>();
		attrResults.put(sourceAttr0,
				ExplainedAttributeMetaData.create(sourceAttr0, Arrays.asList(sourceAttr0Explain), false));
		attrResults.put(sourceAttr1,
				ExplainedAttributeMetaData.create(sourceAttr1, Arrays.asList(sourceAttr1Explain), false));

		Stream<AlgorithmTemplate> templateStream = algorithmTemplateServiceImpl.find(attrResults);

		Map<String, String> model = new HashMap<>();
		model.put(param0Name, sourceAttr0Name);
		model.put(param1Name, sourceAttr1Name);
		AlgorithmTemplate expectedAlgorithmTemplate = new AlgorithmTemplate(script0, model);
		assertEquals(templateStream.collect(Collectors.toList()),
				Stream.of(expectedAlgorithmTemplate).collect(Collectors.toList()));
	}

	@Configuration
	public static class Config
	{
		@Bean
		public AlgorithmTemplateServiceImpl algorithmTemplateServiceImpl()
		{
			return new AlgorithmTemplateServiceImpl(dataService());
		}

		@Bean
		public DataService dataService()
		{
			return mock(DataService.class);
		}
	}
}
