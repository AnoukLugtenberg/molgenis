package org.molgenis.data.mapper.controller;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toMap;
import static org.elasticsearch.common.collect.ImmutableSet.of;
import static org.molgenis.data.mapper.controller.MappingServiceController.URI;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Repository;
import org.molgenis.data.mapper.algorithm.AlgorithmService;
import org.molgenis.data.mapper.data.request.AddTagRequest;
import org.molgenis.data.mapper.data.request.AutoTagRequest;
import org.molgenis.data.mapper.data.request.GetOntologyTermRequest;
import org.molgenis.data.mapper.data.request.MappingServiceRequest;
import org.molgenis.data.mapper.data.request.RemoveTagRequest;
import org.molgenis.data.mapper.mapping.MappingService;
import org.molgenis.data.mapper.mapping.model.AttributeMapping;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.mapping.model.MappingTarget;
import org.molgenis.data.semantic.OntologyTagService;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semantic.SemanticSearchService;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.molgenis.ontology.OntologyService;
import org.molgenis.ontology.repository.model.Ontology;
import org.molgenis.ontology.repository.model.OntologyTerm;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.util.ErrorMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Controller
@RequestMapping(URI)
public class MappingServiceController extends MolgenisPluginController
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingServiceController.class);

	public static final String ID = "mappingservice";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;
	private static final String VIEW_MAPPING_PROJECTS = "view-mapping-projects";
	private static final String VIEW_ATTRIBUTE_MAPPING = "view-attribute-mapping";
	private static final String VIEW_SINGLE_MAPPING_PROJECT = "view-single-mapping-project";
	private static final String VIEW_TAG_WIZARD = "view-tag-wizard";

	@Autowired
	private MolgenisUserService molgenisUserService;

	@Autowired
	private MappingService mappingService;

	@Autowired
	private AlgorithmService algorithmService;

	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private OntologyTagService ontologyTagService;

	@Autowired
	private SemanticSearchService semanticSearchService;

	public MappingServiceController()
	{
		super(URI);
	}

	/**
	 * Initializes the model with all mapping projects and all entities to the model.
	 * 
	 * @param model
	 *            the model to initialized
	 * @return view name of the mapping projects list
	 */
	@RequestMapping
	public String viewMappingProjects(Model model)
	{
		model.addAttribute("mappingProjects", mappingService.getAllMappingProjects());
		model.addAttribute("entityMetaDatas", getEntityMetaDatas());
		model.addAttribute("user", SecurityUtils.getCurrentUsername());
		model.addAttribute("admin", SecurityUtils.currentUserIsSu());
		return VIEW_MAPPING_PROJECTS;
	}

	/**
	 * Adds a new mapping project.
	 * 
	 * @param name
	 *            name of the mapping project
	 * @param targetEntity
	 *            name of the project's first {@link MappingTarget}'s target entity
	 * @return redirect URL for the newly created mapping project
	 */
	@RequestMapping(value = "/addMappingProject", method = RequestMethod.POST)
	public String addMappingProject(@RequestParam("mapping-project-name") String name,
			@RequestParam("target-entity") String targetEntity)
	{
		MappingProject newMappingProject = mappingService.addMappingProject(name, getCurrentUser(), targetEntity);
		// FIXME need to write complete URL else it will use /plugin as root and the molgenis header and footer wont be
		// loaded
		return "redirect:/menu/main/mappingservice/mappingproject/" + newMappingProject.getIdentifier();
	}

	@RequestMapping(value = "/removeMappingProject", method = RequestMethod.POST)
	public String deleteMappingProject(@RequestParam(required = true) String mappingProjectId)
	{
		MappingProject project = mappingService.getMappingProject(mappingProjectId);
		if (hasWritePermission(project))
		{
			LOG.info("Deleting mappingProject " + project.getName());
			mappingService.deleteMappingProject(mappingProjectId);
		}
		return "redirect:/menu/main/mappingservice/";
	}

	@RequestMapping(value = "/removeAttributeMapping", method = RequestMethod.POST)
	public String removeAttributeMapping(@RequestParam(required = true) String mappingProjectId,
			@RequestParam(required = true) String target, @RequestParam(required = true) String source,
			@RequestParam(required = true) String attribute)
	{
		MappingProject project = mappingService.getMappingProject(mappingProjectId);
		if (hasWritePermission(project))
		{
			project.getMappingTarget(target).getMappingForSource(source).deleteAttributeMapping(attribute);
			mappingService.updateMappingProject(project);
		}
		return "redirect:/menu/main/mappingservice/mappingproject/" + project.getIdentifier();
	}

	private boolean hasWritePermission(MappingProject project)
	{
		return hasWritePermission(project, true);
	}

	private boolean hasWritePermission(MappingProject project, boolean logInfractions)
	{
		boolean result = SecurityUtils.currentUserIsSu()
				|| project.getOwner().getUsername().equals(SecurityUtils.getCurrentUsername());
		if (logInfractions && !result)
		{
			LOG.warn("User " + SecurityUtils.getCurrentUsername()
					+ " illegally tried to modify mapping project with id " + project.getIdentifier() + " owned by "
					+ project.getOwner().getUsername());
		}
		return result;
	}

	/**
	 * Adds a new {@link EntityMapping} to an existing {@link MappingTarget}
	 * 
	 * @param target
	 *            the name of the {@link MappingTarget}'s entity to add a source entity to
	 * @param source
	 *            the name of the source entity of the newly added {@link EntityMapping}
	 * @param mappingProjectId
	 *            the ID of the {@link MappingTarget}'s {@link MappingProject}
	 * @return redirect URL for the mapping project
	 */
	@RequestMapping(value = "/addEntityMapping", method = RequestMethod.POST)
	public String addEntityMapping(@RequestParam String mappingProjectId, String target, String source)
	{
		MappingProject project = mappingService.getMappingProject(mappingProjectId);
		if (hasWritePermission(project))
		{
			project.getMappingTarget(target).addSource(dataService.getEntityMetaData(source));
			mappingService.updateMappingProject(project);
		}
		return "redirect:/menu/main/mappingservice/mappingproject/" + mappingProjectId;
	}

	/**
	 * Removes entity mapping
	 * 
	 * @param mappingProjectId
	 *            ID of the mapping project to remove entity mapping from
	 * @param target
	 *            entity name of the mapping target
	 * @param source
	 *            entity name of the mapping source
	 * @return redirect url of the mapping project's page
	 */
	@RequestMapping(value = "/removeEntityMapping", method = RequestMethod.POST)
	public String removeEntityMapping(@RequestParam String mappingProjectId, String target, String source)
	{
		MappingProject project = mappingService.getMappingProject(mappingProjectId);
		if (hasWritePermission(project))
		{
			project.getMappingTarget(target).removeSource(source);
			mappingService.updateMappingProject(project);
		}
		return "redirect:/menu/main/mappingservice/mappingproject/" + mappingProjectId;
	}

	/**
	 * Adds a new {@link AttributeMapping} to an {@link EntityMapping}.
	 * 
	 * @param mappingProjectId
	 *            ID of the mapping project
	 * @param target
	 *            name of the target entity
	 * @param source
	 *            name of the source entity
	 * @param targetAttribute
	 *            name of the target attribute
	 * @param algorithm
	 *            the mapping algorithm
	 * @return redirect URL for the attributemapping
	 */
	@RequestMapping(value = "/saveattributemapping", method = RequestMethod.POST)
	public String saveAttributeMapping(@RequestParam(required = true) String mappingProjectId,
			@RequestParam(required = true) String target, @RequestParam(required = true) String source,
			@RequestParam(required = true) String targetAttribute, @RequestParam(required = true) String algorithm)
	{
		MappingProject mappingProject = mappingService.getMappingProject(mappingProjectId);
		if (hasWritePermission(mappingProject))
		{
			MappingTarget mappingTarget = mappingProject.getMappingTarget(target);
			EntityMapping mappingForSource = mappingTarget.getMappingForSource(source);
			AttributeMapping attributeMapping = mappingForSource.getAttributeMapping(targetAttribute);
			if (attributeMapping == null)
			{
				attributeMapping = mappingForSource.addAttributeMapping(targetAttribute);
			}
			attributeMapping.setAlgorithm(algorithm);
			mappingService.updateMappingProject(mappingProject);
		}
		return "redirect:/menu/main/mappingservice/mappingproject/" + mappingProject.getIdentifier();
	}

	/**
	 * Displays a mapping project.
	 * 
	 * @param identifier
	 *            identifier of the {@link MappingProject}
	 * @param target
	 *            Name of the selected {@link MappingTarget}'s target entity
	 * @param model
	 *            the model
	 * @return View name of the
	 */
	@RequestMapping("/mappingproject/{id}")
	public String viewMappingProject(@PathVariable("id") String identifier,
			@RequestParam(value = "target", required = false) String target, Model model)
	{
		MappingProject project = mappingService.getMappingProject(identifier);
		if (target == null)
		{
			target = project.getMappingTargets().get(0).getName();
		}
		// Fill the model
		model.addAttribute("selectedTarget", target);
		model.addAttribute("mappingProject", project);
		model.addAttribute("entityMetaDatas", getNewSources(project.getMappingTarget(target)));
		model.addAttribute("hasWritePermission", hasWritePermission(project, false));

		return VIEW_SINGLE_MAPPING_PROJECT;
	}

	/**
	 * Creates the integrated entity for a mapping project's target
	 * 
	 * @param mappingProjectId
	 *            ID of the mapping project
	 * @param target
	 *            name of the target of the {@link EntityMapping}
	 * @param newEntityName
	 *            name of the new entity to create
	 * @return redirect URL to the data explorer displaying the newly generated entity
	 */
	@RequestMapping("/createIntegratedEntity")
	public String createIntegratedEntity(@RequestParam String mappingProjectId, @RequestParam String target,
			@RequestParam() String newEntityName, Model model)
	{
		try
		{
			MappingTarget mappingTarget = mappingService.getMappingProject(mappingProjectId).getMappingTarget(target);
			String name = mappingService.applyMappings(mappingTarget, newEntityName);
			return "redirect:/menu/main/dataexplorer?entity=" + name;
		}
		catch (RuntimeException ex)
		{
			model.addAttribute("heading", "Failed to create integrated entity.");
			model.addAttribute("message", ex.getMessage());
			model.addAttribute("href", "/menu/main/mappingservice/mappingproject/" + mappingProjectId);
			return "error-msg";
		}
	}

	/**
	 * Lists the entities that may be added as new sources to this mapping project's selected target
	 * 
	 * @param target
	 *            the selected target
	 * @return
	 */
	private List<EntityMetaData> getNewSources(MappingTarget target)
	{
		return StreamSupport.stream(dataService.getEntityNames().spliterator(), false)
				.filter((name) -> isValidSource(target, name)).map(dataService::getEntityMetaData)
				.collect(Collectors.toList());
	}

	private static boolean isValidSource(MappingTarget target, String name)
	{
		return !target.hasMappingFor(name);
	}

	/**
	 * Displays an {@link AttributeMapping}
	 * 
	 * @param mappingProjectId
	 *            ID of the {@link MappingProject}
	 * @param target
	 *            name of the target entity
	 * @param source
	 *            name of the source entity
	 * @param attribute
	 *            name of the target attribute
	 * @param model
	 *            the model
	 * @return name of the attributemapping view
	 */
	@RequestMapping("/attributeMapping")
	public String viewAttributeMapping(@RequestParam String mappingProjectId, @RequestParam String target,
			@RequestParam String source, @RequestParam String attribute, Model model)
	{
		MappingProject project = mappingService.getMappingProject(mappingProjectId);
		MappingTarget mappingTarget = project.getMappingTarget(target);
		EntityMapping entityMapping = mappingTarget.getMappingForSource(source);
		AttributeMapping attributeMapping = entityMapping.getAttributeMapping(attribute);
		if (attributeMapping == null)
		{
			attributeMapping = entityMapping.addAttributeMapping(attribute);
		}
		model.addAttribute("mappingProject", project);
		model.addAttribute("entityMapping", entityMapping);
		model.addAttribute("attributeMapping", attributeMapping);
		model.addAttribute("hasWritePermission", hasWritePermission(project, false));
		return VIEW_ATTRIBUTE_MAPPING;
	}

	/**
	 * Displays on tag wizard button press
	 * 
	 * @param target
	 *            The target entity name
	 * @param model
	 *            the model
	 * 
	 * @return name of the tag wizard view
	 */
	@RequestMapping("/tagWizard")
	public String viewTagWizard(@RequestParam String target, Model model)
	{
		List<Ontology> ontologies = ontologyService.getOntologies();
		EntityMetaData emd = dataService.getEntityMetaData(target);
		List<AttributeMetaData> attributes = newArrayList(emd.getAttributes());
		Map<String, Multimap<Relation, OntologyTerm>> taggedAttributeMetaDatas = attributes.stream().collect(
				toMap((x -> x.getName()), (x -> ontologyTagService.getTagsForAttribute(emd, x))));

		model.addAttribute("entity", emd);
		model.addAttribute("attributes", attributes);
		model.addAttribute("ontologies", ontologies);
		model.addAttribute("taggedAttributeMetaDatas", taggedAttributeMetaDatas);
		model.addAttribute("relations", Relation.values());

		return VIEW_TAG_WIZARD;
	}

	/**
	 * Add a tag for a single attribute
	 * 
	 * @param request
	 *            the {@link AddTagRequest} containing the entityName, attributeName, relationIRI and ontologyTermIRIs
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/tagattribute")
	public @ResponseBody Map<String, String> addTagAttribute(@Valid @RequestBody AddTagRequest request)
	{
		ontologyTagService.addAttributeTag(request.getEntityName(), request.getAttributeName(),
				request.getRelationIRI(), request.getOntologyTermIRIs());

		List<String> IRIs = request.getOntologyTermIRIs();
		Map<String, String> labelIriMap = new HashMap<String, String>();

		if (IRIs.size() < 2)
		{
			labelIriMap.put(IRIs.get(0), ontologyService.getOntologyTerm(IRIs.get(0)).getLabel());
			return labelIriMap;
		}
		else
		{
			String label = "(";
			for (int i = 0; i < IRIs.size(); i++)
			{
				label = label + ontologyService.getOntologyTerm(IRIs.get(i)).getLabel();
				if (i < (IRIs.size() - 1)) label = label + " and ";
				else label = label + ")";
			}
			labelIriMap.put(StringUtils.join(IRIs, ','), label);
			return labelIriMap;
		}
	}

	/**
	 * Delete a single tag
	 * 
	 * @param request
	 *            the {@link RemoveTagRequest} containing entityName, attributeName, relationIRI and ontologyTermIRI
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deletesingletag")
	public @ResponseBody void deleteSingleTag(@Valid @RequestBody RemoveTagRequest request)
	{
		ontologyTagService.removeAttributeTag(request.getEntityName(), request.getAttributeName(),
				request.getRelationIRI(), request.getOntologyTermIRI());
	}

	/**
	 * Clears all tags from every attribute in the current target entity
	 * 
	 * @param entityName
	 *            The name of the {@link Entity}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/clearalltags")
	public @ResponseBody void clearAllTags(@RequestParam String entityName)
	{
		ontologyTagService.removeAllTagsFromEntity(entityName);
	}

	/**
	 * Automatically tags all attributes in the current entity using Lucene lexical matching
	 * 
	 * @param request
	 *            containing the entityName and selected ontology identifiers
	 * @return A {@link Map} containing key {@link AttributeMetaData} and value {@link List} {@link OntologyTerm} pairs
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/autotagattributes")
	public @ResponseBody Map<AttributeMetaData, List<OntologyTerm>> autoTagAttributes(
			@Valid @RequestBody AutoTagRequest request)
	{
		return semanticSearchService.findTags(request.getEntityName(), request.getOntologyIds());
	}

	/**
	 * Returns ontology terms based on a search term and a selected ontology
	 * 
	 * @param request
	 *            Containing ontology identifiers and a search term
	 * @return A {@link List} of {@link OntologyTerm}s
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/getontologyterms")
	public @ResponseBody List<OntologyTerm> getAllOntologyTerms(@Valid @RequestBody GetOntologyTermRequest request)
	{
		return ontologyService.findOntologyTerms(request.getOntologyIds(), of(request.getSearchTerm()), 100);
	}

	/**
	 * Tests an algoritm by computing it for all entities in the source repository.
	 * 
	 * @param mappingServiceRequest
	 *            the {@link MappingServiceRequest} sent by the client
	 * @return Map with the results and size of the source
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/mappingattribute/testscript", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, Object> testScript(@RequestBody MappingServiceRequest mappingServiceRequest)
	{
		EntityMetaData targetEntityMetaData = dataService
				.getEntityMetaData(mappingServiceRequest.getTargetEntityName());
		AttributeMetaData targetAttribute = targetEntityMetaData != null ? targetEntityMetaData
				.getAttribute(mappingServiceRequest.getTargetAttributeName()) : null;
		Repository sourceRepo = dataService.getRepository(mappingServiceRequest.getSourceEntityName());
		List<Object> calculatedValues = algorithmService.applyAlgorithm(targetAttribute,
				mappingServiceRequest.getAlgorithm(), sourceRepo);
		return ImmutableMap.<String, Object> of("results", calculatedValues, "totalCount", Iterables.size(sourceRepo));
	}

	private List<EntityMetaData> getEntityMetaDatas()
	{
		return Lists.newArrayList(Iterables.transform(dataService.getEntityNames(), dataService::getEntityMetaData));
	}

	@ExceptionHandler(RuntimeException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorMessageResponse handleRuntimeException(RuntimeException e)
	{
		LOG.error(e.getMessage(), e);
		return new ErrorMessageResponse(new ErrorMessageResponse.ErrorMessage(
				"An error occurred. Please contact the administrator.<br />Message:" + e.getMessage()));
	}

	private MolgenisUser getCurrentUser()
	{
		return molgenisUserService.getUser(SecurityUtils.getCurrentUsername());
	}
}
