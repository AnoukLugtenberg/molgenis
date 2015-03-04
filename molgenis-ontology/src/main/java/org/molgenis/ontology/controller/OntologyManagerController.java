package org.molgenis.ontology.controller;

import static org.molgenis.ontology.controller.OntologyManagerController.URI;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.data.Entity;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.molgenis.ontology.OntologyService;
import org.molgenis.ontology.index.OntologyIndexer;
import org.molgenis.ontology.utils.OntologyServiceUtil;
import org.molgenis.util.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(URI)
public class OntologyManagerController extends MolgenisPluginController
{
	public static final String ID = "ontologymanager";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	public static final String ONTOLOGY_MANAGER_PLUGIN = "OntologyManagerPlugin";

	@Autowired
	private FileStore fileStore;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private OntologyIndexer harmonizationIndexer;

	public OntologyManagerController()
	{
		super(URI);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String init(Model model) throws Exception
	{
		return ONTOLOGY_MANAGER_PLUGIN;
	}

	@RequestMapping(value = "/ontology", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> getAllOntologies()
	{
		Map<String, Object> results = new HashMap<String, Object>();
		List<Map<String, Object>> ontologies = new ArrayList<Map<String, Object>>();
		for (Entity entity : ontologyService.getAllOntologyEntities())
		{
			ontologies.add(OntologyServiceUtil.getEntityAsMap(entity));
		}
		results.put("results", ontologies);
		return results;
	}

	@RequestMapping(value = "/remove", method = RequestMethod.POST)
	public String removeOntology(@RequestParam String ontologyUri, Model model)
	{
		try
		{
			harmonizationIndexer.removeOntology(ontologyUri);
			model.addAttribute("removeSuccess", true);
			model.addAttribute("message", "The ontology has been removed!");
		}
		catch (Exception e)
		{
			model.addAttribute("message", "It failed to remove this ontology");
			model.addAttribute("removeSuccess", false);
		}
		return ONTOLOGY_MANAGER_PLUGIN;
	}
}
