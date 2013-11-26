package org.molgenis.charts;

import static org.molgenis.charts.ChartController.URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.molgenis.charts.charttypes.HeatMapChart;
import org.molgenis.charts.charttypes.LineChart;
import org.molgenis.charts.data.DataMatrix;
import org.molgenis.charts.data.XYDataSerie;
import org.molgenis.charts.r.RChartService;
import org.molgenis.charts.requests.HeatMapRequest;
import org.molgenis.charts.requests.LineChartRequest;
import org.molgenis.data.QueryRule;
import org.molgenis.util.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import freemarker.template.TemplateException;

@Controller
@RequestMapping(URI)
@SessionAttributes("fileNames")
public class ChartController
{
	public static final String URI = "/charts";
	private static final Logger logger = Logger.getLogger(ChartController.class);

	private final ChartDataService chartDataService;
	private final RChartService rchartService;
	private final FileStore fileStore;

	@Autowired
	public ChartController(ChartDataService chartDataService, RChartService rchartService, FileStore fileStore)
	{
		if (chartDataService == null) throw new IllegalArgumentException("chartDataService is null");
		if (rchartService == null) throw new IllegalArgumentException("rchartVisualizationService is null");
		if (fileStore == null) throw new IllegalArgumentException("fileStore is null");

		this.chartDataService = chartDataService;
		this.rchartService = rchartService;
		this.fileStore = fileStore;
	}

	/**
	 * List of filenames of a user. User can only view his own files
	 */
	@ModelAttribute("fileNames")
	public List<String> getFileNames(Model model)
	{
		@SuppressWarnings("unchecked")
		List<String> fileNames = (List<String>) model.asMap().get("fileNames");
		if (fileNames == null)
		{
			fileNames = new ArrayList<String>();
			model.addAttribute("fileNames", fileNames);
		}

		return fileNames;
	}

	@RequestMapping("/test")
	public String test(HttpServletRequest request, Model model)
	{
		model.addAttribute("queryString", request.getQueryString());
		return "test";
	}

	@RequestMapping("/line")
	public String renderLineChart(@Valid
	LineChartRequest request, Model model)
	{
		List<QueryRule> queryRules = null;// TODO

		List<XYDataSerie> series = new ArrayList<XYDataSerie>();

		for (int i = 0; i < request.getY().size(); i++)
		{
			XYDataSerie data = chartDataService.getXYDataSerie(request.getEntity(), request.getX(),
					request.getY().get(i), queryRules);
			series.add(data);
		}

		Chart chart = new LineChart(series);
		chart.setTitle(request.getTitle());
		chart.setxLabel(request.getxLabel());
		chart.setyLabel(request.getyLabel());
		chart.setWidth(request.getWidth());
		chart.setHeight(request.getHeight());
		model.addAttribute("chart", chart);
		return "timeseries";
	}

	/**
	 * Gets a file from the filestore.
	 * 
	 * User can only view his own files he created with the charts module
	 * 
	 * 
	 * @param out
	 * @param name
	 * @param extension
	 * @param fileNames
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/get/{name}.{extension}")
	public void getFile(OutputStream out, @PathVariable("name")
	String name, @PathVariable("extension")
	String extension, @ModelAttribute("fileNames")
	List<String> fileNames, HttpServletResponse response) throws IOException
	{
		// User can only see his own charts
		if (!fileNames.contains(name))
		{
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		File f = fileStore.getFile(name + "." + extension);
		if (!f.exists())
		{
			logger.warn("Chart file not found [" + name + "]");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		response.setContentType(MimeTypes.getContentType(extension));

		FileCopyUtils.copy(new FileInputStream(f), out);
	}

	/**
	 * Renders a heatmap with r
	 * 
	 * Returns a piece of javascript that can be retrieved by an html page with an ajax request.
	 * 
	 * The page must have an element with id named 'container'. The svg image will be added to this container element.
	 * 
	 * @param request
	 * @param fileNames
	 * @param model
	 * @return
	 * @throws IOException
	 * @throws TemplateException
	 */
	@RequestMapping("/heatmap")
	public String renderHeatMap(@Valid
	HeatMapRequest request, @ModelAttribute("fileNames")
	List<String> fileNames, Model model) throws IOException, TemplateException
	{
		DataMatrix matrix = chartDataService.getDataMatrix(request.getEntity(), request.getX(), request.getY(),
				request.getQueryRules());

		HeatMapChart chart = new HeatMapChart(matrix);
		chart.setTitle(request.getTitle());
		chart.setxLabel(request.getxLabel());
		chart.setyLabel(request.getyLabel());
		chart.setWidth(request.getWidth());
		chart.setHeight(request.getHeight());

		String chartFileName = rchartService.renderHeatMap(chart);
		fileNames.add(chartFileName);

		model.addAttribute("fileName", chartFileName);
		model.addAttribute("nRow", chart.getData().getRowTargets().size());
		model.addAttribute("nCol", chart.getData().getColumnTargets().size());

		return "heatmap";
	}
}
