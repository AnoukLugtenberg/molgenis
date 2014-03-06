package org.molgenis.omx.biobankconnect.algorithm;

import static org.molgenis.omx.biobankconnect.algorithm.AlgorithmEditorController.URI;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.molgenis.omx.biobankconnect.ontologyannotator.OntologyAnnotator;
import org.molgenis.omx.biobankconnect.ontologymatcher.OntologyMatcher;
import org.molgenis.omx.biobankconnect.ontologymatcher.OntologyMatcherRequest;
import org.molgenis.omx.biobankconnect.wizard.BiobankConnectWizard;
import org.molgenis.omx.biobankconnect.wizard.ChooseCataloguePage;
import org.molgenis.omx.biobankconnect.wizard.CurrentUserStatus;
import org.molgenis.omx.biobankconnect.wizard.OntologyAnnotatorPage;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.ObservationSet;
import org.molgenis.search.Hit;
import org.molgenis.search.SearchResult;
import org.molgenis.search.SearchService;
import org.molgenis.security.user.UserAccountService;
import org.molgenis.ui.wizard.AbstractWizardController;
import org.molgenis.ui.wizard.Wizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Iterables;

@Controller
@RequestMapping(URI)
public class AlgorithmEditorController extends AbstractWizardController
{

	public static final String ID = "algorithm";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String PROTOCOL_IDENTIFIER = "store_mapping";

	@Autowired
	private OntologyMatcher ontologyMatcher;
	@Autowired
	private OntologyAnnotator ontologyAnnotator;
	@Autowired
	private UserAccountService userAccountService;
	@Autowired
	private CurrentUserStatus currentUserStatus;
	@Autowired
	private AlgorithmUnitConverter algorithmUnitConverter;
	@Autowired
	private AlgorithmScriptLibrary algorithmScriptLibrary;
	@Autowired
	private AlgorithmGenerator algorithmGenerator;
	@Autowired
	private ApplyAlgorithms applyAlgorithms;
	@Autowired
	private SearchService searchService;

	private BiobankConnectWizard wizard;
	private final DataService dataService;
	private final ChooseBiobankPage chooseBiobanksPage;
	private final OntologyAnnotatorPage ontologyAnnotatorPage;
	private final ChooseCataloguePage chooseCataloguePage;
	private final AlgorithmEditorPage algorithmEditorPage;
	private final AlgorithmGeneratorPage algorithmGeneratorPage;

	@Autowired
	public AlgorithmEditorController(ChooseBiobankPage chooseBiobanksPage, OntologyAnnotatorPage ontologyAnnotatorPage,
			ChooseCataloguePage chooseCataloguePage, AlgorithmEditorPage algorithmEditorPage,
			AlgorithmGeneratorPage algorithmGeneratorPage, DataService dataService)
	{
		super(URI, ID);
		if (algorithmEditorPage == null) throw new IllegalArgumentException("algorithmEditorPage is null!");
		if (chooseBiobanksPage == null) throw new IllegalArgumentException("chooseBiobanksPage is null!");
		if (chooseCataloguePage == null) throw new IllegalArgumentException("chooseCataloguePage is null!");
		if (ontologyAnnotatorPage == null) throw new IllegalArgumentException("ontologyAnnotatorPage is null!");
		if (algorithmGeneratorPage == null) throw new IllegalArgumentException("algorithmGeneratorPage is null!");
		if (dataService == null) throw new IllegalArgumentException("dataService is null!");
		this.chooseBiobanksPage = chooseBiobanksPage;
		this.ontologyAnnotatorPage = ontologyAnnotatorPage;
		this.chooseCataloguePage = chooseCataloguePage;
		this.algorithmEditorPage = algorithmEditorPage;
		this.algorithmGeneratorPage = algorithmGeneratorPage;
		this.dataService = dataService;
	}

	@Override
	public void onInit(HttpServletRequest request)
	{
		wizard.setDataSets(getBiobankDataSets());
		currentUserStatus.setUserLoggedIn(userAccountService.getCurrentUser().getUsername(),
				request.getRequestedSessionId());
	}

	@Override
	protected Wizard createWizard()
	{
		wizard = new BiobankConnectWizard();
		wizard.setDataSets(getBiobankDataSets());
		wizard.setUserName(userAccountService.getCurrentUser().getUsername());
		wizard.addPage(chooseCataloguePage);
		wizard.addPage(ontologyAnnotatorPage);
		wizard.addPage(chooseBiobanksPage);
		wizard.addPage(algorithmEditorPage);
		wizard.addPage(algorithmGeneratorPage);
		return wizard;
	}

	private List<DataSet> getBiobankDataSets()
	{
		List<DataSet> dataSets = new ArrayList<DataSet>();
		Iterable<DataSet> allDataSets = dataService.findAll(DataSet.ENTITY_NAME, DataSet.class);
		for (DataSet dataSet : allDataSets)
		{
			if (dataSet.getProtocolUsed().getIdentifier().equals(PROTOCOL_IDENTIFIER)) continue;
			if (dataSet.getIdentifier().matches("^" + userAccountService.getCurrentUser().getUsername() + ".*derived$")) continue;
			dataSets.add(dataSet);
		}
		return dataSets;
	}

	@RequestMapping(value = "/annotate", method = RequestMethod.POST)
	public String annotate(HttpServletRequest request)
	{
		ontologyAnnotator.removeAnnotations(wizard.getSelectedDataSet().getId());
		if (request.getParameter("selectedOntologies") != null)
		{
			List<String> documentTypes = new ArrayList<String>();
			for (String ontologyUri : request.getParameter("selectedOntologies").split(","))
			{
				documentTypes.add("ontologyTerm-" + ontologyUri);
			}
			ontologyAnnotator.annotate(wizard.getSelectedDataSet().getId(), documentTypes);
		}
		return init(request);
	}

	@RequestMapping(value = "/annotate/remove", method = RequestMethod.POST)
	public String removeAnnotations(HttpServletRequest request) throws Exception
	{
		ontologyAnnotator.removeAnnotations(wizard.getSelectedDataSet().getId());
		return init(request);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/createmapping", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public SearchResult createMappings(@RequestBody
	OntologyMatcherRequest request)
	{
		String userName = userAccountService.getCurrentUser().getUsername();
		List<Integer> selectedDataSetIds = request.getSelectedDataSetIds();
		if (selectedDataSetIds.size() > 0)
		{
			return ontologyMatcher.generateMapping(userName, request.getFeatureId(), request.getTargetDataSetId(),
					selectedDataSetIds.get(0));
		}
		return new SearchResult(0, Collections.<Hit> emptyList());
	}

	@RequestMapping(method = RequestMethod.POST, value = "/suggestscript", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> suggestScript(@RequestBody
	OntologyMatcherRequest request)
	{
		Map<String, Object> jsonResults = new HashMap<String, Object>();
		String userName = userAccountService.getCurrentUser().getUsername();
		String suggestedScript = algorithmGenerator.generateAlgorithm(userName, request);
		jsonResults.put("suggestedScript", suggestedScript);
		return jsonResults;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/testscript", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> testScrpit(@RequestBody
	OntologyMatcherRequest request)
	{
		if (request.getSelectedDataSetIds().size() == 0 || request.getAlgorithmScript().isEmpty()) return Collections
				.emptyMap();
		Map<String, Object> jsonResults = new HashMap<String, Object>();

		DataSet sourceDataSet = dataService.findOne(DataSet.ENTITY_NAME, request.getSelectedDataSetIds().get(0),
				DataSet.class);
		Iterable<ObservationSet> observationSets = dataService.findAll(ObservationSet.ENTITY_NAME,
				new QueryImpl().eq(ObservationSet.PARTOFDATASET, sourceDataSet), ObservationSet.class);

		ObservableFeature feature = dataService.findOne(ObservableFeature.ENTITY_NAME, request.getFeatureId(),
				ObservableFeature.class);

		Collection<Object> results = applyAlgorithms.createValueFromAlgorithm(feature.getDataType(),
				request.getSelectedDataSetIds().get(0), request.getAlgorithmScript()).values();

		jsonResults.put("results", results);
		jsonResults.put("totalCounts", Iterables.size(observationSets));
		return jsonResults;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/savescript", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, String> saveScript(@RequestBody
	OntologyMatcherRequest request)
	{
		String userName = userAccountService.getCurrentUser().getUsername();
		if (request.getSelectedDataSetIds().size() > 0)
		{
			return ontologyMatcher.updateScript(userName, request);
		}
		return new HashMap<String, String>();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/progress", produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> progress(@RequestBody
	OntologyMatcherRequest request)
	{
		Map<String, Object> jsonResults = new HashMap<String, Object>();
		String userName = userAccountService.getCurrentUser().getUsername();
		jsonResults.put("isRunning", currentUserStatus.isUserMatching(userName));
		jsonResults.put("percentage", currentUserStatus.getPercentageOfProcessForUser(userName));
		return jsonResults;
	}

	@Override
	@ModelAttribute("javascripts")
	public List<String> getJavascripts()
	{
		return Arrays.asList("bootstrap-fileupload.min.js", "jquery-ui-1.9.2.custom.min.js", "common-component.js",
				"catalogue-chooser.js", "ontology-annotator.js", "ontology-matcher.js", "mapping-manager.js",
				"algorithm-editor.js", "biobank-connect.js", "jstat.min.js", "d3.min.js", "vega.min.js",
				"biobankconnect-graph.js");
	}

	@Override
	@ModelAttribute("stylesheets")
	public List<String> getStylesheets()
	{
		return Arrays.asList("bootstrap-fileupload.min.css", "jquery-ui-1.9.2.custom.min.css", "biobank-connect.css",
				"catalogue-chooser.css", "ontology-matcher.css", "mapping-manager.css", "ontology-annotator.css",
				"algorithm-editor.css");
	}
}