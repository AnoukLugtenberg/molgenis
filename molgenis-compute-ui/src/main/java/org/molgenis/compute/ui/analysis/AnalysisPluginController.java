package org.molgenis.compute.ui.analysis;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.molgenis.compute.ui.IdGenerator;
import org.molgenis.compute.ui.clusterexecutor.ClusterManager;
import org.molgenis.compute.ui.meta.AnalysisJobMetaData;
import org.molgenis.compute.ui.meta.AnalysisMetaData;
import org.molgenis.compute.ui.meta.AnalysisTargetMetaData;
import org.molgenis.compute.ui.meta.UIBackendMetaData;
import org.molgenis.compute.ui.meta.UIWorkflowMetaData;
import org.molgenis.compute.ui.model.*;
import org.molgenis.compute5.CommandLineRunContainer;
import org.molgenis.compute5.ComputeCommandLine;
import org.molgenis.compute5.ComputeProperties;
import org.molgenis.compute5.GeneratedScript;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.framework.ui.MolgenisPluginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

@Controller
@RequestMapping(AnalysisPluginController.URI)
public class AnalysisPluginController extends MolgenisPluginController
{
	private static Logger logger = Logger.getLogger(AnalysisPluginController.class);

	public static final String ID = "analysis";
	public static final String URI = MolgenisPluginController.PLUGIN_URI_PREFIX + ID;

	private String runID = "testEmpty12";
	private String path = ".tmp" + File.separator + runID + File.separator;
	private String pathProtocols = path + "protocols" + File.separator;

	private static final String WORKFLOW_DEFAULT = "workflow.csv";
	private static final String PARAMETERS_DEFAULT = "parameters.csv";
	private static final String WORKSHEET = "worksheet.csv";

	private String extension = ".sh";

	private List<String> writtenProtocols = null;

	private final DataService dataService;

	@Autowired
	private ClusterManager clusterManager;

	private String url, scheduler;

	@Autowired
	public AnalysisPluginController(DataService dataService)
	{
		super(URI);
		this.dataService = dataService;
	}

	@RequestMapping(method = GET)
	public String init(Model model, @RequestParam(value = "analysis", required = false) String analysisId)
	{
		if (analysisId != null)
		{
			Analysis analysis = dataService.findOne(AnalysisMetaData.INSTANCE.getName(), analysisId, Analysis.class);
			if (analysis == null) throw new UnknownEntityException("Unknown Analysis [" + analysisId + "]");

			long nrTargets = dataService.count(AnalysisTargetMetaData.INSTANCE.getName(),
					new QueryImpl().eq(AnalysisTargetMetaData.ANALYSIS, analysis));

			model.addAttribute("analysis", analysis);
			model.addAttribute("hasAnalysisTargets", nrTargets > 0);
		}

		Iterable<UIWorkflow> workflows = dataService.findAll(UIWorkflowMetaData.INSTANCE.getName(), UIWorkflow.class);
		model.addAttribute("workflows", workflows);

		return "view-analysis";
	}

	@RequestMapping(value = "/view/{analysisId}", method = GET)
	public String viewAnalysis(Model model, @PathVariable(value = "analysisId") String analysisId)
	{
		Analysis analysis = dataService.findOne(AnalysisMetaData.INSTANCE.getName(), analysisId, Analysis.class);
		if (analysis == null) throw new UnknownEntityException("Unknown Analysis [" + analysisId + "]");

		long nrTargets = dataService.count(AnalysisTargetMetaData.INSTANCE.getName(),
				new QueryImpl().eq(AnalysisTargetMetaData.ANALYSIS, analysis));

		model.addAttribute("analysis", analysis);
		model.addAttribute("hasAnalysisTargets", nrTargets > 0);

		Iterable<UIWorkflow> workflows = dataService.findAll(UIWorkflowMetaData.INSTANCE.getName(), UIWorkflow.class);
		model.addAttribute("workflows", workflows);

		return "view-analysis";
	}

	@RequestMapping(value = "/create", method = GET)
	public String createAnalysis(Model model, @RequestParam(value = "workflow", required = false) String workflowId,
			@RequestParam(value = "target", required = false) String targetEntityName,
			@RequestParam(value = "q", required = false) String query)
	{
		// TODO discuss how to select backend
		Iterable<UIBackend> backends = dataService.findAll(UIBackendMetaData.INSTANCE.getName(), UIBackend.class);
		if (Iterables.isEmpty(backends))
		{
			throw new RuntimeException("Database does not contain any backend");
		}
		UIBackend backend = backends.iterator().next();
		Date creationDate = new Date();

		UIWorkflow workflow;
		if (workflowId != null && !workflowId.isEmpty())
		{
			workflow = dataService.findOne(UIWorkflowMetaData.INSTANCE.getName(), workflowId, UIWorkflow.class);
			if (workflow == null)
			{
				throw new UnknownEntityException("Unknown " + UIWorkflow.class.getSimpleName() + " [" + workflowId
						+ "]");
			}

		}
		else
		{
			// TODO discuss how to select initial workflow if not specified
			Iterable<UIWorkflow> workflows = dataService.findAll(UIWorkflowMetaData.INSTANCE.getName(),
					UIWorkflow.class);
			if (Iterables.isEmpty(backends))
			{
				throw new RuntimeException("Database does not contain any workflows");
			}
			workflow = workflows.iterator().next();

		}

		String analysisId = IdGenerator.generateId();
		String analysisName = "Analysis-" + creationDate.getTime();
		final Analysis analysis = new Analysis(analysisId, analysisName);
		analysis.setBackend(backend);
		analysis.setCreationDate(creationDate);
		analysis.setWorkflow(workflow);
		dataService.add(AnalysisMetaData.INSTANCE.getName(), analysis);

		if (targetEntityName != null && !targetEntityName.isEmpty())
		{
			Iterable<Entity> targets;
			if (query != null)
			{
				targets = dataService.findAll(targetEntityName);
			}
			else
			{
				// FIXME apply query
				targets = dataService.findAll(targetEntityName);
			}

			dataService.add(AnalysisTargetMetaData.INSTANCE.getName(),
					Iterables.transform(targets, new Function<Entity, AnalysisTarget>()
					{
						@Override
						public AnalysisTarget apply(Entity entity)
						{
							return new AnalysisTarget(IdGenerator.generateId(), entity.getIdValue().toString(),
									analysis);
						}
					}));
		}

		return "forward:" + AnalysisPluginController.URI + "/view/" + analysisId;
	}

	@RequestMapping(value = "/create/{analysisId}/target/{targetId}", method = POST)
	@ResponseStatus(HttpStatus.OK)
	public void createAnalysisTarget(@PathVariable(value = "analysisId") String analysisId,
			@PathVariable(value = "targetId") String targetId)
	{
		Analysis analysis = dataService.findOne(AnalysisMetaData.INSTANCE.getName(), analysisId, Analysis.class);
		if (analysis == null) throw new UnknownEntityException("Unknown Analysis [" + analysisId + "]");

		AnalysisTarget analysisTarget = new AnalysisTarget(IdGenerator.generateId(), targetId, analysis);
		dataService.add(AnalysisTargetMetaData.INSTANCE.getName(), analysisTarget);
	}

	@RequestMapping(value = "/run/{analysisId}", method = POST)
	@ResponseStatus(HttpStatus.OK)
	public void runAnalysis(@PathVariable(value = "analysisId") String analysisId)
	{
		Analysis analysis = dataService.findOne(AnalysisMetaData.INSTANCE.getName(), analysisId, Analysis.class);
		if (analysis == null) throw new UnknownEntityException("Unknown Analysis [" + analysisId + "]");
		logger.info("Running analysis [" + analysisId + "]");

		try
		{
			UIWorkflow uiWorkflow = analysis.getWorkflow();

			String workflowFile = uiWorkflow.getWorkflowFile();
			String parametersFile = uiWorkflow.getParametersFile();

			FileUtils.writeStringToFile(new File(path + WORKFLOW_DEFAULT), workflowFile);
			FileUtils.writeStringToFile(new File(path + PARAMETERS_DEFAULT), parametersFile);

			CsvWriter csvWriter = new CsvWriter(new File(path + WORKSHEET), ',');
			try
			{
				Iterable<AnalysisTarget> targets = dataService.findAll(AnalysisTargetMetaData.INSTANCE.getName(),
						new QueryImpl().eq(AnalysisTargetMetaData.ANALYSIS, analysis), AnalysisTarget.class);
				if (targets == null || Iterables.isEmpty(targets))
				{
					throw new UnknownEntityException("Expected at least one analysis target");
				}

				String targetEntityName = analysis.getWorkflow().getTargetType();

				EntityMetaData metaData = dataService.getEntityMetaData(targetEntityName);
				csvWriter.writeAttributeNames(Iterables.transform(metaData.getAtomicAttributes(),
						new Function<AttributeMetaData, String>()
						{
							@Override
							public String apply(AttributeMetaData attribute)
							{
								System.out.println("attribute: " + attribute.getName());
								return attribute.getName();
							}
						}));

				Iterable<Entity> entities = dataService.findAll(targetEntityName,
						Iterables.transform(targets, new Function<AnalysisTarget, Object>()
						{

							@Override
							public Object apply(AnalysisTarget analysisTarget)
							{
								System.out.println("analysis target value: " + analysisTarget.getIdValue());
								return analysisTarget.getTargetId();
							}
						}));
				for (Entity entity : entities)
				{
					csvWriter.add(entity);
				}
			}
			finally
			{
				csvWriter.close();
			}
			List<UIWorkflowNode> nodes = uiWorkflow.getNodes();

			writtenProtocols = new ArrayList<String>();
			for (UIWorkflowNode node : nodes)
			{
				UIWorkflowProtocol protocol = node.getProtocol();
				String protocolName = protocol.getName();
				String template = protocol.getTemplate();

				if (!isWritten(protocolName))
				{
					// FileUtils.writeStringToFile(new File(pathProtocols + protocolName + extension), template);
					FileUtils.writeStringToFile(new File(path + protocolName), template);
					writtenProtocols.add(protocolName);
				}
			}

			readUserProperties();
			String[] args =
			{ "--generate", "--workflow", path + WORKFLOW_DEFAULT, "--parameters", path + PARAMETERS_DEFAULT,
					"--parameters", path + WORKSHEET, "-b", scheduler, "--runid", runID, "--weave", "--url",
					url, "--path", "", "-rundir", path + "rundir" };

			ComputeProperties properties = new ComputeProperties(args);
			properties.execute = false;
			properties.runDir = path + "rundir";
			CommandLineRunContainer container = new ComputeCommandLine().execute(properties);

			List<GeneratedScript> generatedScripts = container.getTasks();
			List<AnalysisJob> jobs = new ArrayList<AnalysisJob>();
			for (GeneratedScript generatedScript : generatedScripts)
			{
				AnalysisJob job = new AnalysisJob(IdGenerator.generateId());
				job.setName(generatedScript.getName());
				job.setGeneratedScript(generatedScript.getScript());

				UIWorkflowNode node = findNode(uiWorkflow, generatedScript.getStepName());
				job.setWorkflowNode(node);
			    job.setStatus(JobStatus.GENERATED);
				jobs.add(job);

				dataService.add(AnalysisJobMetaData.INSTANCE.getName(), job);
			}

			// update analysis
			analysis.setSubmitScsript(container.getSumbitScript());
			analysis.setJobs(jobs);
			dataService.update(AnalysisMetaData.INSTANCE.getName(), analysis);

		}
		catch (IOException e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}

		clusterManager.executeAnalysis(analysis);

	}

	@RequestMapping(value = "/stop/{analysisId}", method = POST)
	@ResponseStatus(HttpStatus.OK)
	public void stopAnalysis(@PathVariable(value = "analysisId") String analysisId)
	{
		// TODO implement stop analysis
		logger.info("TODO implement stop analysis");
	}

	private UIWorkflowNode findNode(UIWorkflow uiWorkflow, String stepName)
	{
		for (UIWorkflowNode node : uiWorkflow.getNodes())
		{
			String name = node.getName();
			if (stepName.equalsIgnoreCase(name)) return node;
		}
		return null;
	}

	private boolean isWritten(String protocolName)
	{
		return writtenProtocols.contains(protocolName);
	}

	private void readUserProperties()
	{
		Properties prop = new Properties();
		InputStream input = null;

		try
		{
			input = new FileInputStream(".cluster.properties");

			// load a properties file
			prop.load(input);

			url = prop.getProperty(ClusterManager.URL);
			scheduler = prop.getProperty(ClusterManager.SCHEDULER);

		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			if (input != null)
			{
				try
				{
					input.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

}
