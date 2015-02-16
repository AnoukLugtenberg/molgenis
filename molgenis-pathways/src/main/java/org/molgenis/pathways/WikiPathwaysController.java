package org.molgenis.pathways;

import static org.molgenis.pathways.WikiPathwaysController.URI;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Valid;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Repository;
import org.molgenis.dataWikiPathways.WSPathway;
import org.molgenis.dataWikiPathways.WSPathwayInfo;
import org.molgenis.dataWikiPathways.WSSearchResult;
import org.molgenis.dataWikiPathways.WikiPathwaysPortType;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;

@Controller
@RequestMapping(URI)
public class WikiPathwaysController extends MolgenisPluginController
{
	private static final String COLORS2 = "colors";
	private static final String GRAPH_IDS = "graphIds";
	private static final String PATHWAY_ID = "pathwayId";
	// FIXME: proper exception handling

	public static final String ID = "wikipathways";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	public final WikiPathwaysPortType service;
	public static final String organism = "Homo sapiens";
	public Map<String, List<String>> nodeList = new HashMap<>();
	public String geneSymbol = "";
	public String pwGPML = "";
	public Map<String, Integer> genes = new HashMap<>();
	public static Map<Integer, String> variantColor = new HashMap<>();
	private Map<String, String> pathwayNames;
	public Map<String, List<String>> allGenesPerPathway = new HashMap<>();
	public static final File idsAndGpml = new File("/Users/molgenis/Documents/idsAndGpml.csv");

	int countUniqueVcfGenes = 0;
	int countUniquePwGenes = 0;
	int countHighImpact = 0;
	int countModerateImpact = 0;
	int countLowImpact = 0;

	private final LoadingCache<String, List<WSPathwayInfo>> ALL_PATHWAY_CACHED = CacheBuilder.newBuilder()
			.maximumSize(Integer.MAX_VALUE).refreshAfterWrite(1, TimeUnit.DAYS)
			.build(new CacheLoader<String, List<WSPathwayInfo>>()
			{
				@Override
				public List<WSPathwayInfo> load(String organism) throws Exception
				{
					List<WSPathwayInfo> listPathways = service.listPathways(organism);
					// asyncWikiPathwayLoader.asyncLoadPathways(listPathways, PATHWAY_IMAGE_CACHED);
					return listPathways;

				}
			});

	private final LoadingCache<String, byte[]> PATHWAY_IMAGE_CACHED = CacheBuilder.newBuilder()
			.maximumSize(Integer.MAX_VALUE).refreshAfterWrite(1, TimeUnit.DAYS).build(new CacheLoader<String, byte[]>()
			{
				@Override
				public synchronized byte[] load(String pathwayId) throws Exception
				{
					return service.getPathwayAs("svg", pathwayId, 0);

				}
			});

	private final LoadingCache<List<String>, List<WSSearchResult>> ALL_VCF_PATHWAY_CACHED = CacheBuilder.newBuilder()
			.maximumSize(Integer.MAX_VALUE).refreshAfterWrite(1, TimeUnit.DAYS)
			.build(new CacheLoader<List<String>, List<WSSearchResult>>()
			{
				public List<WSSearchResult> load(List<String> genesForPathwaySearch) throws Exception
				{
					
					List<WSSearchResult> listPathways = service.findPathwaysByXref(genesForPathwaySearch, Collections.singletonList("H")); // H for HGNC database (human gene symbols)
					return listPathways;

				}
			});

	private final LoadingCache<Map<String, Object>, byte[]> COLORED_PATHWAY_IMAGE_CACHED = CacheBuilder.newBuilder()
			.maximumSize(Integer.MAX_VALUE).refreshAfterWrite(1, TimeUnit.DAYS)
			.build(new CacheLoader<Map<String, Object>, byte[]>()
			{
				@SuppressWarnings(
				{ "rawtypes", "unchecked" })
				public byte[] load(Map<String, Object> coloredPathwayParameters) throws Exception
				{
					List a = (List) coloredPathwayParameters.get(GRAPH_IDS);
					List b = (List) coloredPathwayParameters.get(COLORS2);
					return service.getColoredPathway(coloredPathwayParameters.get(PATHWAY_ID).toString(), "0", a, b,
							"svg");
				}
			});

	@Autowired
	private AsyncWikiPathwayLoader asyncWikiPathwayLoader;

	@Autowired
	private DataService dataService;

	@Autowired
	public WikiPathwaysController(WikiPathwaysPortType service)
	{
		super(URI);
		this.service = service;
	}

	@RequestMapping(method = GET)
	public String init(Model model) throws IOException, ExecutionException
	{
		model.addAttribute("listOfPathwayNames", pathwayNames);

		Iterable<EntityMetaData> entitiesMeta = Iterables.transform(dataService.getEntityNames(),
				new Function<String, EntityMetaData>()
				{
					@Override
					public EntityMetaData apply(String entityName)
					{
						return dataService.getEntityMetaData(entityName);
					}
				});
		model.addAttribute("entitiesMeta", entitiesMeta);
		model.addAttribute("selectedEntityName", "");

		return "view-WikiPathways";
	}

	// No spring annotations, used by methods in this class
	@RequestMapping(value = "/allPathways", method = POST)
	@ResponseBody
	private Map<String, String> getListOfPathwayNames() throws ExecutionException, IOException, SAXException,
			ParserConfigurationException
	{
		Map<String, String> pathwayNames = new HashMap<String, String>();
		Long t1 = System.currentTimeMillis();
		List<WSPathwayInfo> listOfPathways = ALL_PATHWAY_CACHED.get(organism);

		for (WSPathwayInfo info : listOfPathways)
		{
			pathwayNames.put(info.getId(), info.getName());
		}

		Long t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);

		// getAllGenesPerPathway(pathwayNames);

		return pathwayNames;
	}

	// public Map<String, List<String>> getAllGenesPerPathway(Map<String, String> pathwayNames) throws IOException,
	// SAXException, ParserConfigurationException
	// {
	// System.out.println("in method getAllGenesPerPathway()");
	//
	// String everyGene = "";
	// List<String> genesForRanking = new ArrayList<>();
	// List<String> allPwIds = new ArrayList<>();
	//
	// for (String id : pathwayNames.keySet())
	// {
	// allPwIds.add(id);
	// }
	// pwGPML = getAllPathwayGpml(allPwIds);

	// DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	// DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	// InputStream is = new ByteArrayInputStream(pwGPML.getBytes());
	// Document doc = dBuilder.parse(is);
	// NodeList dataNodes = doc.getElementsByTagName("DataNode");
	//
	// for (int i = 0; i < dataNodes.getLength(); i++)
	// {
	// Element dataNode = (Element) dataNodes.item(i);
	// everyGene = dataNode.getAttribute("TextLabel");
	// // System.out.println(textLabel);
	//
	// }
	//
	// Pattern pat = Pattern.compile("^[0-9A-Za-z\\-]*");
	// if (everyGene.contains("&quot;"))
	// {
	// System.out.println("WARNING: textlabel(" + everyGene + ") contains quotes, removing those...");
	// everyGene = everyGene.replace("&quot;", "");// FIXME: nasty construction, but wikipathways data is not
	// // consistent. How to do this properly
	// }
	//
	// for (String id : pathwayNames.keySet())
	// {
	// Matcher mat = pat.matcher(everyGene);
	// if (mat.find())
	// {
	// genesForRanking.add(mat.group(0));
	// allGenesPerPathway.put(id, genesForRanking);
	// }
	// }
	// return genesForRanking;

	// idsAndGpml = writeToFile(allGenesPerPathway);
	// System.out.println("pathway id: " + allGenesPerPathway.keySet());

	// return allGenesPerPathway;
	// return null;
	//
	// }
	//
	// public String getAllPathwayGpml(List<String> allPwIds)
	// {
	// // System.out.println("in method getAllGpmlFiles()");
	//
	// for (String id : allPwIds)
	// {
	// // System.out.println(id);
	// WSPathway pw = service.getPathway(id, 0);
	// pwGPML = pw.getGpml();
	// }
	//
	// return pwGPML;
	//
	// }

	// public File writeToFile(Map<String, List<String>> allGPMLs) throws IOException
	// {
	// if (!idsAndGpml.exists())
	// {
	// idsAndGpml.createNewFile();
	// }
	//
	// FileWriter fw = new FileWriter(idsAndGpml.getAbsoluteFile());
	// BufferedWriter bw = new BufferedWriter(fw);
	//
	// for (String id : allGPMLs.keySet())
	// {
	// bw.write(id + "\t" + allGPMLs.get(id) + "\n");
	// }
	// bw.close();
	//
	// return idsAndGpml;
	// }

	// With spring annotation, can be called via url by, for example javascript
	@RequestMapping(value = "/geneName", method = POST)
	@ResponseBody
	public Map<String, String> getPathwayByGeneName(@Valid @RequestBody String submittedGene)
	{
		Map<String, String> pathwayNames2 = new HashMap<String, String>();
		List<WSSearchResult> listOfPathways2 = service.findPathwaysByText(submittedGene, organism);

		for (WSSearchResult info2 : listOfPathways2)
		{
			pathwayNames2.put(info2.getId(), info2.getName());
		}

		return pathwayNames2;
	}

	@RequestMapping(value = "/pathwayViewer/{pathwayId}", method = GET)
	@ResponseBody
	public String getPathway(@PathVariable String pathwayId) throws MalformedURLException, ExecutionException
	{

		byte[] source = PATHWAY_IMAGE_CACHED.get(pathwayId);
		ByteArrayInputStream bis = new ByteArrayInputStream(source);

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(bis);
		scanner.useDelimiter("\\Z");// To read all scanner content in one String
		String pathway = "";
		if (scanner.hasNext()) pathway = scanner.next();

		return pathway;
	}

	@RequestMapping(value = "/vcfFile", method = POST)
	@ResponseBody
	public void readVcfFile(@Valid @RequestBody String selectedVcf)
	{
		List<String> allGeneSymbols = new ArrayList<String>();
		variantColor = new HashMap<>();
		genes = new HashMap<>();
		Repository repository = dataService.getRepositoryByEntityName(selectedVcf);
		Pattern p = Pattern.compile("([A-Z]*\\|)(\\|*[0-9]+\\||\\|+)+([0-9A-Z]+)(\\|*)(.*)");
		String geneSymbol = "";
		// int count = 0;
		Iterator<Entity> iterator = repository.iterator();
		System.out.println("gaat nu naar while loop");
		while (iterator.hasNext())
		{
			// count += 1;
			Entity entity = iterator.next();
			String eff = entity.getString("EFF");
			Matcher m = p.matcher(eff);
			if (m.find())
			{
				geneSymbol = m.group(3);
				System.out.println("gevonden gensymbool: " + geneSymbol);
			}
			else
			{
				continue;
			}
			int impact = eff.contains("HIGH") ? 3 : eff.contains("MODERATE") ? 2 : eff.contains("LOW") ? 1 : 0;
			allGeneSymbols.add(geneSymbol);

			if (genes.containsKey(geneSymbol))
			{
				if (impact > genes.get(geneSymbol))
				{
					genes.put(geneSymbol, impact);
				}
			}
			else
			{
				genes.put(geneSymbol, impact);
			}

//			for (String symbol : allGeneSymbols)
//			{
//				int occurrence = StringUtils.countOccurrencesOf(allGeneSymbols.toString(), symbol);
				// rankingList.put(symbol, occurrence);
//			}

		}
		System.out.println("genes with impact: " + genes);
		System.out.println("all unique genes with highest impact: " + genes);
		// System.out.println(rankingList);
		// System.out.println(count);

		variantColor.put(3, "FF0000"); // red
		variantColor.put(2, "FFA500"); // orange
		variantColor.put(1, "FFFF00"); // yellow
		// variantColor.put(0, "0000FF"); // blue
		variantColor.put(0, "219AD7"); // lighter blue, so the gene symbol is still visible
	}

	@RequestMapping(value = "/pathwaysByGenes", method = POST)
	@ResponseBody
	public Map<String, String> getListOfPathwayNamesByGenes() throws ExecutionException, ParserConfigurationException,
			SAXException, IOException
	{
		Map<String, String> pathwayByGenes = new HashMap<String, String>();
		List<String> geneSymbols = new ArrayList<String>();
		System.out.println("the size of genes is: " + genes.size());
		
		for (String symbol : genes.keySet())
		{
			geneSymbols.add(symbol);
		}

		List<String> temporaryList = new ArrayList<String>();
		// String query = "";
		List<String> genesForPathwaySearch = new ArrayList<String>();
		System.out.println("the size of geneSymbols is: " + geneSymbols.size());
		for (int i = 0; i < geneSymbols.size(); i++)
		{
			String e = geneSymbols.get(i);
			temporaryList.add(e);

			if (i % 20 == 0 && i != 0)
			{
				System.out.println("processing genes");
				// genesToPathways(pathwayByGenes, temporaryList, query);
				genesToPathways(pathwayByGenes, temporaryList, genesForPathwaySearch);
				temporaryList.clear();
			}
		}
		if (temporaryList.size() != 0)
		{
			// genesToPathways(pathwayByGenes, temporaryList, query);
			genesToPathways(pathwayByGenes, temporaryList, genesForPathwaySearch);
		}
		return pathwayByGenes;
	}

	// private void genesToPathways(Map<String, String> pathwayByGenes, List<String> temporaryList, String query)
	private void genesToPathways(Map<String, String> pathwayByGenes, List<String> temporaryList,
			List<String> genesForPathwaySearch) throws ExecutionException, ParserConfigurationException, SAXException,
			IOException
	{
		List<WSSearchResult> listOfPathwaysByGenes = new ArrayList<WSSearchResult>();
		for (String gene : temporaryList)
		{
			// query += gene + " ";
			genesForPathwaySearch.add(gene);
		}
		// listOfPathwaysByGenes = service.findPathwaysByText(query, organism);
		// List<String> codes = new ArrayList<String>();
		// codes.add("H"); // H for HGNC database (human gene symbols)
		listOfPathwaysByGenes = ALL_VCF_PATHWAY_CACHED.get(genesForPathwaySearch);
		for (WSSearchResult info3 : listOfPathwaysByGenes)
		{
			// getGPML(info3.getId());
			// List<String>genesFromNodeList = new ArrayList<>();
			// genesFromNodeList.addAll(nodeList.keySet());

			if (info3.getSpecies().equals("Homo sapiens"))
			{
				pathwayByGenes.put(info3.getId(), info3.getName() + " (" + info3.getId() + ")");
			}

		}
	}

	@RequestMapping(value = "/getGPML/{pathwayId}", method = GET)
	@ResponseBody
	public String getGPML(@PathVariable String pathwayId) throws ParserConfigurationException, SAXException,
			IOException, ExecutionException
	{
		nodeList = new HashMap<>();
		WSPathway wsPathway = service.getPathway(pathwayId, 0);
		String gpml = wsPathway.getGpml();
		// System.out.println(gpml);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputStream is = new ByteArrayInputStream(gpml.getBytes());
		Document doc = dBuilder.parse(is);
		NodeList dataNodes = doc.getElementsByTagName("DataNode");

		for (int i = 0; i < dataNodes.getLength(); i++)
		{
			Element dataNode = (Element) dataNodes.item(i);
			String graphId = dataNode.getAttribute("GraphId");
			// graphIdList.add(graphId);
			String textLabel = dataNode.getAttribute("TextLabel");
			// System.out.println(textLabel);

			geneSymbol = getGeneSymbol(textLabel);
			if (graphId.isEmpty())
			{
				continue;
			}
			if (nodeList.containsKey(geneSymbol))
			{ // if a gene is already in the map
				nodeList.get(geneSymbol).add(graphId); // get the list, and add the graphId
			}
			else
			{
				List<String> graphIdList = new ArrayList<String>();
				graphIdList.add(graphId);
				nodeList.put(geneSymbol, graphIdList);
			}

		}
		System.out.println(nodeList);

		return getColoredPathway(pathwayId, nodeList);
	}

	String getGeneSymbol(String textLabel)
	{
		String geneSymbols = "";
		Pattern pat = Pattern.compile("^[0-9A-Za-z\\-]*");
		if (textLabel.contains("&quot;"))
		{
			System.out.println("WARNING: textlabel(" + textLabel + ") contains quotes, removing those...");
			textLabel = textLabel.replace("&quot;", "");// FIXME: nasty construction, but wikipathways data is not
														// consistent. How to do this properly
		}
		Matcher mat = pat.matcher(textLabel);
		if (mat.find())
		{
			geneSymbols = mat.group(0);
		}
		else
		{
			return null;
		}
		return geneSymbols;
	}

	private String getColoredPathway(String pathwayId, Map<String, List<String>> nodeList) throws ExecutionException
	{
		Map<Integer, List<String>> graphIdToColor = new HashMap<Integer, List<String>>();
		List<String> colors = new ArrayList<String>();
		List<String> graphIds = new ArrayList<String>();
		byte[] base64Binary = null;

		// genes: geneSymbol, impact
		// variantColor: impact, color
		// nodeList: geneSymbol, graphId

		for (String gene : genes.keySet())
		{
			int impact = genes.get(gene);
			// System.out.println("NODELIST KEYS: " + nodeList.keySet());
			// System.out.println("GENES KEYS: " + genes.keySet());
			if (nodeList.containsKey(gene))
			{
				if (graphIdToColor.containsKey(impact))
				{ // Impact already in graphIdToColor
					graphIdToColor.get(impact).addAll(nodeList.get(gene)); // get list of graphIds for this impact and
																			// add other graphIds
				}
				else
				{
					graphIdToColor.put(impact, nodeList.get(gene)); // put new impact and first graphIds in map
				}
			}
		}

		for (int j : graphIdToColor.keySet())
		{
			// For each graphId for this impact, we put the impact color in the color list
			for (int i = 0; i < graphIdToColor.get(j).size(); i++)
			{
				colors.add(variantColor.get(j));
				// graphIds.add(graphIdToColor.get(impact).get(i));
			}

			graphIds.addAll(graphIdToColor.get(j));

		}

		System.out.println(graphIds + " " + colors);

		// Calculation for ranking the pathways
		countUniqueVcfGenes = genes.keySet().size();
		countUniquePwGenes = nodeList.keySet().size();

		int countH = 0;
		int countM = 0;
		int countL = 0;
		System.out.println(genes);
		for (String gene : genes.keySet())
		{
			int impact = genes.get(gene);

			if (impact == 3)
			{
				countH += 1;
				countHighImpact = countHighImpact + countH;
			}
			else if (impact == 2)
			{
				countM += 1;
				countModerateImpact = countModerateImpact + countM;
			}
			else if (impact == 1)
			{
				countL += 1;
				countLowImpact = countLowImpact + countL;
			}
			else
			{
				continue;
			}
		}

		System.out.println(countUniquePwGenes + " unique pathway genes");
		System.out.println(countUniqueVcfGenes + " unique vcf genes");
		System.out.println(countHighImpact + " high");
		System.out.println(countModerateImpact + " moderate");
		System.out.println(countLowImpact + " low");

		// base64Binary = service.getPathway();

		// Check if graphIds and colors are empty
		if (!graphIds.isEmpty() && !colors.isEmpty())
		{
			Map<String, Object> coloredPathwayParameters = new HashMap<String, Object>();
			Long t3 = System.currentTimeMillis();
			coloredPathwayParameters.put(PATHWAY_ID, pathwayId);
			coloredPathwayParameters.put(GRAPH_IDS, graphIds);
			coloredPathwayParameters.put(COLORS2, colors);

			// System.out.println(coloredPathwayParameters);
			base64Binary = COLORED_PATHWAY_IMAGE_CACHED.get(coloredPathwayParameters);
			// base64Binary = idsToPathways(graphIds, colors, pathwayId);
			ByteArrayInputStream bis = new ByteArrayInputStream(base64Binary);

			Scanner scan = new Scanner(bis);
			scan.useDelimiter("\\Z");// To read all scanner content in one String
			String coloredPathway = "";
			if (scan.hasNext()) coloredPathway = scan.next();
			scan.close();
			Long t4 = System.currentTimeMillis();
			System.out.println(t4 - t3);
			return coloredPathway.replace("<svg", "<svg viewBox='0 0 1000 1500'");
		}
		else
		{
			System.out.println("normal pathway, uncolored");
			Long t5 = System.currentTimeMillis();
			// if graphIds and colors are empty, getPathway() -> uncolored
			byte[] uncoloredPathway = PATHWAY_IMAGE_CACHED.get(pathwayId);
			ByteArrayInputStream byteis = new ByteArrayInputStream(uncoloredPathway);

			Scanner scanner = new Scanner(byteis);
			scanner.useDelimiter("\\Z");// To read all scanner content in one String
			String regularPathway = "";
			if (scanner.hasNext()) regularPathway = scanner.next();
			scanner.close();
			Long t6 = System.currentTimeMillis();
			System.out.println(t6 - t5);
			return regularPathway;
		}
	}

	// private byte[] idsToPathways(List<String> graphIds, List<String> colors, String pathwayId)
	// {
	// System.out.println(graphIds + " " + colors);
	//
	// byte[] base64Binary = service.getColoredPathway(pathwayId, "0", graphIds, colors, "svg");
	// byte[] base64Binary = service.getColoredPathway(pathwayId, "0", Arrays.asList(new String[]{"cf3", "cd6"}),
	// Arrays.asList(new String[]{"FFA500", "FF0000"}), "svg");

	// return base64Binary;
	// }

	// "FF0000" red
	// "FFA500" orange
	// "FFFF00" yellow
	// "0000FF" blue

}