package org.molgenis.data.annotation.impl;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.support.MapEntity;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CosmicServiceAnnotatorTest
{
	private EntityMetaData metaDataCanAnnotate;
	private EntityMetaData metaDataCantAnnotate;
	private CosmicServiceAnnotator annotator;
	private AttributeMetaData attributeMetaDataCanAnnotate;
	private AttributeMetaData attributeMetaDataCantAnnotate;
	private AttributeMetaData attributeMetaDataCantAnnotate2;
	private Entity entity;
	private HttpClient httpClient;
	private static String SERVICE_RESPONSE;
	private ArrayList<Entity> input;

	@BeforeMethod
	public void beforeMethod()
	{
		annotator = new CosmicServiceAnnotator();

		metaDataCanAnnotate = mock(EntityMetaData.class);
		attributeMetaDataCanAnnotate = mock(AttributeMetaData.class);
		when(attributeMetaDataCanAnnotate.getName()).thenReturn("ensemblId");
		when(attributeMetaDataCanAnnotate.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));

		when(metaDataCanAnnotate.getAttribute("ensemblId")).thenReturn(attributeMetaDataCanAnnotate);

		metaDataCantAnnotate = mock(EntityMetaData.class);
		attributeMetaDataCantAnnotate = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotate.getName()).thenReturn("otherID");
		when(attributeMetaDataCantAnnotate.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.STRING.toString().toLowerCase()));
		attributeMetaDataCantAnnotate2 = mock(AttributeMetaData.class);
		when(attributeMetaDataCantAnnotate2.getName()).thenReturn("ensemblId");
		when(attributeMetaDataCantAnnotate2.getDataType()).thenReturn(
				MolgenisFieldTypes.getType(FieldTypeEnum.DATE.toString().toLowerCase()));

		when(metaDataCantAnnotate.getAttribute("ensemblId")).thenReturn(attributeMetaDataCantAnnotate2);

		entity = mock(Entity.class);
		when(entity.get("ensemblId")).thenReturn("ENSG00000186092");
		input = new ArrayList<Entity>();
		input.add(entity);

		this.httpClient = mock(HttpClient.class);

		SERVICE_RESPONSE = "[{\"ID\":\"COSM911918\",\"feature_type\":\"somatic_variation\",\"alt_alleles\":[\"C\",\"A\"],\"end\":69345,\"seq_region_name\":\"1\",\"consequence_type\":\"synonymous_variant\",\"strand\":1,\"start\":69345},{\"ID\":\"COSM426644\",\"feature_type\":\"somatic_variation\",\"alt_alleles\":[\"G\",\"T\"],\"end\":69523,\"seq_region_name\":\"1\",\"consequence_type\":\"missense_variant\",\"strand\":1,\"start\":69523},{\"ID\":\"COSM75742\",\"feature_type\":\"somatic_variation\",\"alt_alleles\":[\"G\",\"A\"],\"end\":69538,\"seq_region_name\":\"1\",\"consequence_type\":\"missense_variant\",\"strand\":1,\"start\":69538}]";
	}

	@Test
	public void annotate() throws IllegalStateException, IOException
	{
		List<Entity> expectedList = new ArrayList<Entity>();
		Map<String, Object> resultMap1 = new LinkedHashMap<String, Object>();
		resultMap1.put("ID", "COSM911918");
		resultMap1.put("feature_type", "somatic_variation");
		resultMap1.put("alt_alleles", "C,A");
		resultMap1.put("end", 69345);
		resultMap1.put("seq_region_name", "1");
		resultMap1.put("consequence_type", "synonymous_variant");
		resultMap1.put("strand", 1);
		resultMap1.put("start", 69345);
		resultMap1.put(CosmicServiceAnnotator.ENSEMBLE_ID, "ENSG00000186092");

		Map<String, Object> resultMap2 = new LinkedHashMap<String, Object>();
		resultMap2.put("ID", "COSM426644");
		resultMap2.put("feature_type", "somatic_variation");
		resultMap2.put("alt_alleles", "G,T");
		resultMap2.put("end", 69523);
		resultMap2.put("seq_region_name", "1");
		resultMap2.put("consequence_type", "missense_variant");
		resultMap2.put("strand", 1);
		resultMap2.put("start", 69523);
		resultMap2.put(CosmicServiceAnnotator.ENSEMBLE_ID, "ENSG00000186092");

		Map<String, Object> resultMap3 = new LinkedHashMap<String, Object>();
		resultMap3.put("ID", "COSM75742");
		resultMap3.put("feature_type", "somatic_variation");
		resultMap3.put("alt_alleles", "G,A");
		resultMap3.put("end", 69538);
		resultMap3.put("seq_region_name", "1");
		resultMap3.put("consequence_type", "missense_variant");
		resultMap3.put("strand", 1);
		resultMap3.put("start", 69538);
		resultMap3.put(CosmicServiceAnnotator.ENSEMBLE_ID, "ENSG00000186092");

		Entity expectedEntity1 = new MapEntity(resultMap1);
		Entity expectedEntity2 = new MapEntity(resultMap2);
		Entity expectedEntity3 = new MapEntity(resultMap3);

		expectedList.add(expectedEntity1);
		expectedList.add(expectedEntity2);
		expectedList.add(expectedEntity3);

		Iterator<Entity> expected = expectedList.iterator();

		InputStream ServiceStream = new ByteArrayInputStream(SERVICE_RESPONSE.getBytes(Charset.forName("UTF-8")));
		HttpEntity catalogReleaseEntity = when(mock(HttpEntity.class).getContent()).thenReturn(ServiceStream).getMock();
		HttpResponse catalogReleaseResponse = when(mock(HttpResponse.class).getEntity()).thenReturn(
				catalogReleaseEntity).getMock();
		StatusLine statusLine = when(mock(StatusLine.class).getStatusCode()).thenReturn(200).getMock();
		when(catalogReleaseResponse.getStatusLine()).thenReturn(statusLine);

		when(httpClient.execute(argThat(new BaseMatcher<HttpGet>()
		{
			@Override
			public boolean matches(Object item)
			{
				return ((HttpGet) item)
						.getURI()
						.toString()
						.equals("http://beta.rest.ensembl.org/feature/id/ENSG00000186092.json?feature=somatic_variation");
			}

			@Override
			public void describeTo(Description description)
			{
				throw new UnsupportedOperationException();
			}
		}))).thenReturn(catalogReleaseResponse);

		Iterator<Entity> results = annotator.annotate(input.iterator());

		assertEquals(results.next(), expectedEntity1);
		assertEquals(results.next(), expectedEntity2);
		assertEquals(results.next(), expectedEntity3);
	}

	@Test
	public void canAnnotateTrueTest()
	{
		assertEquals(annotator.canAnnotate(metaDataCanAnnotate), true);
	}

	@Test
	public void canAnnotateFalseTest()
	{
		assertEquals(annotator.canAnnotate(metaDataCantAnnotate), false);
	}
}
