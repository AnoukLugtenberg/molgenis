package org.molgenis.dataexplorer.controller;

import static org.molgenis.dataexplorer.controller.DataExplorerController.URI;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.molgenis.MolgenisFieldTypes.FieldTypeEnum;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Repository;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.support.MapEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.entityexplorer.controller.EntityExplorerController;
import org.molgenis.framework.server.MolgenisPermissionService;
import org.molgenis.framework.server.MolgenisPermissionService.Permission;
import org.molgenis.framework.server.MolgenisSettings;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.search.Hit;
import org.molgenis.search.SearchRequest;
import org.molgenis.search.SearchResult;
import org.molgenis.search.SearchService;
import org.molgenis.util.GsonHttpMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Controller class for the data explorer.
 * 
 * The implementation javascript file for the resultstable is defined in a MolgenisSettings property named
 * 'dataexplorer.resultstable.js' possible values are '/js/SingleObservationSetTable.js' or
 * '/js/MultiObservationSetTable.js' with '/js/MultiObservationSetTable.js' as the default
 * 
 * @author erwin
 * 
 */
@Controller
@RequestMapping(URI)
public class DataExplorerController extends MolgenisPluginController
{
	private static final Logger logger = Logger.getLogger(DataExplorerController.class);

	public static final String ID = "dataexplorer";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	private static final int DOWNLOAD_SEARCH_LIMIT = 1000;

	private static final String DEFAULT_KEY_TABLE_TYPE = "MultiObservationSetTable.js";
	private static final String KEY_TABLE_TYPE = "dataexplorer.resultstable.js";
	private static final String KEY_APP_HREF_CSS = "app.href.css";

	// Including excluding the charts module
	public static final boolean INCLUDE_CHARTS_MODULE = true;
	public static final String KEY_APP_INCLUDE_CHARTS = "app.dataexplorer.include.charts";
	private static final String MODEL_APP_INCLUDE_CHARTS = "app_dataexplorer_include_charts";

	public static final String INITLOCATION = "initLocation";
	public static final String COORDSYSTEM = "coordSystem";
	public static final String CHAINS = "chains";
	public static final String SOURCES = "sources";
	public static final String BROWSERLINKS = "browserLinks";
	public static final String SEARCHENDPOINT = "searchEndpoint";
	public static final String KARYOTYPEENDPOINT = "karyotypeEndpoint";
	public static final String GENOMEBROWSERTABLE = "genomeBrowserTable";

	public static final String MUTATION_START_POSITION = "start_nucleotide";
	public static final String MUTATION_ID = "mutation_id";
	public static final String MUTATION_CHROMOSOME = "chromosome";

	@Autowired
	private DataService dataService;

	@Autowired
	private MolgenisPermissionService molgenisPermissionService;

	@Autowired
	private MolgenisSettings molgenisSettings;

	@Autowired
	private SearchService searchService;

	public DataExplorerController()
	{
		super(URI);
	}

	/**
	 * TODO JJ
	 * 
	 * Show the explorer page
	 * 
	 * @param model
	 * @return the view name
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String init(@RequestParam(value = "dataset", required = false) String selectedEntityName, Model model)
			throws Exception
	{
		// set entityExplorer URL for link to EntityExplorer for x/mrefs, but only if the user has permission to see the
		// plugin
		if (molgenisPermissionService.hasPermissionOnPlugin(EntityExplorerController.ID, Permission.READ)
				|| molgenisPermissionService.hasPermissionOnPlugin(EntityExplorerController.ID, Permission.WRITE))
		{
			model.addAttribute("entityExplorerUrl", EntityExplorerController.ID);
		}

		Iterable<String> names = dataService.getEntityNames();
		List<String> entitiesNames = Lists.newArrayList(names.iterator());
		if (entitiesNames.isEmpty()) throw new IllegalArgumentException("Entities names are not found");
		model.addAttribute("entitiesNames", entitiesNames);

		if (selectedEntityName == null)
		{
			selectedEntityName = entitiesNames.get(0);
		}
		model.addAttribute("selectedEntityName", selectedEntityName);

		// Init genome browser
		model.addAttribute("genomeBrowserSets", getGenomeBrowserSetsToModel());

		String resultsTableJavascriptFile = molgenisSettings.getProperty(KEY_TABLE_TYPE, DEFAULT_KEY_TABLE_TYPE);
		model.addAttribute("resultsTableJavascriptFile", resultsTableJavascriptFile);

		String appHrefCss = molgenisSettings.getProperty(KEY_APP_HREF_CSS);
		if (appHrefCss != null) model.addAttribute(KEY_APP_HREF_CSS.replaceAll("\\.", "_"), appHrefCss);

		// including/excluding charts
		Boolean appIncludeCharts = molgenisSettings.getBooleanProperty(KEY_APP_INCLUDE_CHARTS, INCLUDE_CHARTS_MODULE);
		model.addAttribute(MODEL_APP_INCLUDE_CHARTS, appIncludeCharts);

		model.addAttribute(INITLOCATION, molgenisSettings.getProperty(INITLOCATION));
		model.addAttribute(COORDSYSTEM, molgenisSettings.getProperty(COORDSYSTEM));
		model.addAttribute(CHAINS, molgenisSettings.getProperty(CHAINS));
		model.addAttribute(SOURCES, molgenisSettings.getProperty(SOURCES));
		model.addAttribute(BROWSERLINKS, molgenisSettings.getProperty(BROWSERLINKS));
		model.addAttribute(SEARCHENDPOINT, molgenisSettings.getProperty(SEARCHENDPOINT));
		model.addAttribute(KARYOTYPEENDPOINT, molgenisSettings.getProperty(KARYOTYPEENDPOINT));
		model.addAttribute(GENOMEBROWSERTABLE, molgenisSettings.getProperty(GENOMEBROWSERTABLE));

		return "view-dataexplorer";
	}

	private Map<String, String> getGenomeBrowserSetsToModel()
	{
		Map<String, String> genomeBrowserSets = new HashMap<String, String>();
		for (String entityName : dataService.getEntityNames())
		{
			Repository repository = dataService.getRepositoryByEntityName(entityName);
			AttributeMetaData attributeStartPosition = repository.getAttribute(MUTATION_START_POSITION);
			AttributeMetaData attributeId = repository.getAttribute(MUTATION_ID);
			AttributeMetaData attributeChromosome = repository.getAttribute(MUTATION_CHROMOSOME);
			if (attributeStartPosition != null && attributeId != null && attributeChromosome != null)
			{
				genomeBrowserSets.put(entityName, repository.getLabel());
			}
		}
		return genomeBrowserSets;
	}

	@RequestMapping(value = "/download", method = POST)
	public void download(@RequestParam("searchRequest") String searchRequest, HttpServletResponse response)
			throws IOException
	{
		searchRequest = URLDecoder.decode(searchRequest, "UTF-8");
		logger.info("Download request: [" + searchRequest + "]");

		SearchRequest request = new GsonHttpMessageConverter().getGson().fromJson(searchRequest, SearchRequest.class);
		request.getQuery().pageSize(DOWNLOAD_SEARCH_LIMIT).offset(0);

		response.setContentType("text/csv");
		response.addHeader("Content-Disposition", "attachment; filename=" + getCsvFileName(request.getDocumentType()));

		CsvWriter writer = null;
		try
		{
			writer = new CsvWriter(response.getWriter());

			// The fieldsToReturn contain identifiers, we need the names
			Map<String, String> nameByIdentifier = getFeatureNames(request.getFieldsToReturn());

			// Keep order
			List<String> names = new ArrayList<String>();
			for (String identifier : request.getFieldsToReturn())
			{
				String name = nameByIdentifier.get(identifier);
				if (name != null)
				{
					names.add(name);
				}
			}

			writer.writeAttributeNames(names);
			int count = 0;
			SearchResult searchResult;

			do
			{
				request.getQuery().offset(count);
				searchResult = searchService.search(request);

				for (Hit hit : searchResult.getSearchHits())
				{
					Entity entity = new MapEntity();
					for (String field : request.getFieldsToReturn())
					{
						entity.set(nameByIdentifier.get(field), hit.getColumnValueMap().get(field));
					}
					writer.add(entity);
				}

				count += searchResult.getSearchHits().size();
			}
			while (count < searchResult.getTotalHitCount());
		}
		finally
		{
			IOUtils.closeQuietly(writer);
		}
	}

	@RequestMapping(value = "/aggregate", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public AggregateResponse aggregate(@Valid @RequestBody AggregateRequest request)
	{
		// TODO create utility class to extract info from entity/attribute uris
		String[] attributeUriTokens = request.getAttributeUri().split("/");
		String entityName = attributeUriTokens[3];
		String attributeName = attributeUriTokens[5];

		Map<String, Integer> aggregateMap = new HashMap<String, Integer>();
		for (Entity entity : dataService.findAll(entityName))
		{
			String val = entity.getString(attributeName);
			Integer count = aggregateMap.get(val);
			if (count == null) aggregateMap.put(val, 1);
			else aggregateMap.put(val, count + 1);
		}
		return new AggregateResponse(aggregateMap);
	}

	@RequestMapping(value = "/filterdialog", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	public String filterwizard(@RequestBody @Valid FilterWizardRequest request, Model model)
	{
		// TODO create utility class to extract info from entity/attribute uris
		String[] entityUriTokens = request.getEntityUri().split("/");
		String entityName = entityUriTokens[entityUriTokens.length - 1];

		Repository repository = dataService.getRepositoryByEntityName(entityName);
		Iterable<AttributeMetaData> attributeMetaDataIterable = Iterables.filter(repository.getLevelOneAttributes(),
				new Predicate<AttributeMetaData>()
				{
					@Override
					public boolean apply(AttributeMetaData attributeMetaData)
					{
						if (attributeMetaData.getDataType().getEnumType() == FieldTypeEnum.HAS)
						{
							attributeMetaData.getRefEntity().getLevelOneAttributes();
						}
						return attributeMetaData.getDataType().getEnumType() == FieldTypeEnum.HAS;
					}
				});

		List<EntityMetaData> entityMetaDataGroups = new ArrayList<EntityMetaData>();
		entityMetaDataGroups.add(repository);
		for (AttributeMetaData attributeMetaData : attributeMetaDataIterable)
		{
			entityMetaDataGroups.add(attributeMetaData.getRefEntity());
		}

		model.addAttribute("entityMetaDataGroups", entityMetaDataGroups);
		return "view-filter-dialog";
	}

	// Get feature names by feature identifiers
	private Map<String, String> getFeatureNames(List<String> identifiers)
	{
		Iterable<ObservableFeature> features = dataService.findAll(ObservableFeature.ENTITY_NAME,
				new QueryImpl().in(ObservableFeature.IDENTIFIER, identifiers), ObservableFeature.class);

		Map<String, String> nameByIdentifier = new LinkedHashMap<String, String>();
		for (ObservableFeature feature : features)
		{
			nameByIdentifier.put(feature.getIdentifier(), feature.getName());
		}

		return nameByIdentifier;
	}

	private String getCsvFileName(String dataSetName)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dataSetName + "_" + dateFormat.format(new Date()) + ".csv";
	}
}
