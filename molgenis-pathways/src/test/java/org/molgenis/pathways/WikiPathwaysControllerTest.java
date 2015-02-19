package org.molgenis.pathways;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.mockito.Mockito;
import org.molgenis.pathways.WikiPathwaysController;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wikipathways.client.WikiPathwaysClient;
import org.xml.sax.SAXException;

public class WikiPathwaysControllerTest
{
	private WikiPathwaysController controller;
	private WikiPathwaysClient serviceMock;
	
	@BeforeTest
	public void init()
	{
		serviceMock = Mockito.mock(WikiPathwaysClient.class);
		controller = new WikiPathwaysController(new WikiPathwaysService(serviceMock));
	}
	
	@Test
	public void testGetGeneSymbol()
	{
		assertEquals(controller.getGeneSymbol("TUSC2 / Fus1 , Fusion"), "TUSC2");
		assertEquals(controller.getGeneSymbol("MIR9-1"), "MIR9-1");
		assertEquals(controller.getGeneSymbol("GENE[cytosol]"), "GENE");
		assertEquals(controller.getGeneSymbol("TUSC2abc/adsf"), "TUSC2abc");

	}
	
	@Test
	public void testGetGPML() throws ParserConfigurationException, SAXException, IOException, ConverterException
	{
		// mock inprogrammeren
		WSPathway pathway = Mockito.mock(WSPathway.class);
		when(pathway.getGpml()).thenReturn("<gpml>  "
				+ "<DataNode TextLabel='TUSC2 / Fus1 , Fusion' GraphId = 'cf7548' Type='GeneProduct' GroupRef='bced7'>" 
				+ "<Graphics CenterX='688.6583271016858' CenterY='681.6145075824545' Width='80.0' Height='20.0' ZOrder='32768' FontSize='10' Valign='Middle' />"
				+ "<Xref Database='Ensembl' ID='ENSG00000197081' />"
				+ "</DataNode>"
				+ "<DataNode TextLabel='IPO4' GraphId='d9af5' Type='GeneProduct' GroupRef='bced7'>"
				+ "<Graphics CenterX='688.6583271016858' CenterY='701.6145075824545' Width='80.0' Height='20.0' ZOrder='32768' FontSize='10' Valign='Middle' />"
				+ "<Xref Database='Ensembl' ID='ENSG00000196497' />"
				+ "</DataNode></gpml>");
		when(serviceMock.getPathway("WP2377", 0)).thenReturn(pathway);
				
//		controller.getGPML("WP2377");
		
//		assertEquals(controller.nodeList, ImmutableMap.<String, String>of("TUSC2","cf7548","IPO4","d9af5"));
	}
	
	@Test
	public void testGetColoredPathway() throws RemoteException
	{
		// mock inprogrammeren
		byte[] base64Binary = null;
		
		when(serviceMock.getColoredPathway("WP2377", "0", Arrays.asList(new String[]{"cf3", "cd6"}), Arrays.asList(new String[]{"FFA500", "FF0000"}), "svg")).thenReturn(base64Binary);
		
	}
}
