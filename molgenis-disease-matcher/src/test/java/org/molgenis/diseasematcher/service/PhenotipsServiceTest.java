package org.molgenis.diseasematcher.service;

import static org.testng.AssertJUnit.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.testng.annotations.Test;

public class PhenotipsServiceTest
{
	@Test
	public void testBuildQueryURIString() throws UnsupportedEncodingException
	{
		PhenotipsService ps = new PhenotipsService();
		ArrayList<String> terms = new ArrayList<String>();
		terms.add("HP:0000252");
		terms.add("HP:0004322");
		terms.add("HP:0009900");
		String uri = ps.buildQueryURIString(terms);

		String targetUri = "http://playground.phenotips.org/bin/get/PhenoTips/OmimPredictService?q=1&format=html&limit=500&symptom=HP:0000252&symptom=HP:0004322&symptom=HP:0009900";
		assertEquals(targetUri, uri);
	}
}
