package org.molgenis.charts.data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.charts.BoxPlotChart;
import org.molgenis.charts.ChartDataService;
import org.molgenis.charts.MolgenisAxisType;
import org.molgenis.charts.MolgenisChartException;
import org.molgenis.charts.XYDataChart;
import org.molgenis.charts.calculations.BoxPlotCalcUtil;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.Queryable;
import org.molgenis.data.Repository;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.model.MolgenisModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ChartDataServiceImpl implements ChartDataService
{
	private final DataService dataService;
	private static final Logger logger = Logger.getLogger(ChartDataServiceImpl.class);

	@Autowired
	public ChartDataServiceImpl(DataService dataService)
	{
		if (dataService == null) throw new IllegalArgumentException("dataService is null");
		this.dataService = dataService;

	}
	
	@Override
	public XYDataChart getXYDataChart(String entityName, String attributeNameXaxis, String attributeNameYaxis, String split, List<QueryRule> queryRules) 
	{
		Repository<? extends Entity> repo = dataService.getRepositoryByEntityName(entityName);
		
		try
		{			
			final Class<?> attributeXJavaType = repo.getAttribute(attributeNameXaxis).getDataType().getJavaType();
			final Class<?> attributeYJavaType = repo.getAttribute(attributeNameYaxis).getDataType().getJavaType();
			final List<XYDataSerie> xYDataSeries;
			if(StringUtils.isNotBlank(split)){
				logger.info("getXYDataChart() --- without split");
				xYDataSeries = Arrays.asList(this.getXYDataSerie(entityName, attributeNameXaxis, attributeNameYaxis, attributeXJavaType, attributeYJavaType, queryRules));
			}else{
				logger.info("getXYDataChart() --- with split");
				xYDataSeries = this.getXYDataSeries(entityName, attributeNameXaxis, attributeNameYaxis, attributeXJavaType, attributeYJavaType, split, queryRules);
			} 
			return new XYDataChart(xYDataSeries, MolgenisAxisType.getType(attributeXJavaType), MolgenisAxisType.getType(attributeYJavaType));
		}
		catch (MolgenisModelException e)
		{
			throw new MolgenisChartException("Error creating a xYDataChart, error: " + e);
		}
	}
	
	
	@Override
	public XYDataSerie getXYDataSerie(
			String entityName, 
			String attributeNameXaxis, 
			String attributeNameYaxis,
			Class<?> attributeXJavaType,
			Class<?> attributeYJavaType,
			List<QueryRule> queryRules)
	{		
		Repository<? extends Entity> repo = dataService.getRepositoryByEntityName(entityName);

		XYDataSerie serie = new XYDataSerie();
		serie.setName(
				repo.getAttribute(attributeNameXaxis).getLabel() + 
				" vs " + 
				repo.getAttribute(attributeNameYaxis).getLabel());
		serie.setAttributeXJavaType(attributeXJavaType);
		serie.setAttributeYJavaType(attributeYJavaType);		

		Sort sort = new Sort(Sort.DEFAULT_DIRECTION, attributeNameXaxis, attributeNameYaxis);
		Iterable<? extends Entity> iterable = getIterable(entityName, queryRules, sort);
		for (Entity entity : iterable)
		{
			Object x = getJavaEntityValue(entity, attributeNameXaxis, attributeXJavaType);
			Object y = getJavaEntityValue(entity, attributeNameYaxis, attributeYJavaType);
			serie.addData(new XYData(x, y));
		}
		
		return serie;
	}
	
	@Override
	public List<XYDataSerie> getXYDataSeries(
			String entityName, 
			String attributeNameXaxis, 
			String attributeNameYaxis,
			Class<?> attributeXJavaType,
			Class<?> attributeYJavaType,
			String split,
			List<QueryRule> queryRules)
	{		
		Sort sort = new Sort(Sort.DEFAULT_DIRECTION, attributeNameXaxis, attributeNameYaxis);
		Iterable<? extends Entity> iterable = getIterable(entityName, queryRules, sort);
		
		Map<String, XYDataSerie> xYDataSeriesMap = new HashMap<String, XYDataSerie>();
		for (Entity entity : iterable)
		{
			String splitValue = split + "__" + entity.get(split);
			if(!xYDataSeriesMap.containsKey(splitValue))
			{
				XYDataSerie serie = new XYDataSerie();
				serie.setName(splitValue);
				serie.setAttributeXJavaType(attributeXJavaType);
				serie.setAttributeYJavaType(attributeYJavaType);
				xYDataSeriesMap.put(splitValue, serie);
			}
			
			Object x = getJavaEntityValue(entity, attributeNameXaxis, attributeXJavaType);
			Object y = getJavaEntityValue(entity, attributeNameYaxis, attributeYJavaType);
			xYDataSeriesMap.get(splitValue).addData(new XYData(x, y));
		}
		
		List<XYDataSerie> series = new ArrayList<XYDataSerie>();
		for(Entry<String, XYDataSerie> serie: xYDataSeriesMap.entrySet()) {
			series.add(serie.getValue());
		}

		return series;
	}
	
	@Override
	public BoxPlotChart getBoxPlotChart(String entityName, String attributeName, List<QueryRule> queryRules, String split)
	{	
		final List<BoxPlotSerie> boxPlotSeries;
		if(StringUtils.isNotBlank(split))
		{
			boxPlotSeries = Arrays.asList(getBoxPlotSerie(entityName, attributeName, queryRules));
		} 
		else 
		{
			boxPlotSeries = getBoxPlotSeries(entityName, attributeName, queryRules, split);
		}
		
		return new BoxPlotChart(boxPlotSeries);
	}
	
	@Override
	public BoxPlotSerie getBoxPlotSerie(
			String entityName,
			String attributeName,
			List<QueryRule> queryRules
			){
		Sort sort = new Sort(Sort.DEFAULT_DIRECTION, attributeName);
		Iterable<? extends Entity> iterable = getIterable(entityName, queryRules, sort);
		List<Double> list = new ArrayList<Double>(); 
		for (Entity entity : iterable)
		{
			list.add(entity.getDouble(attributeName));
		}
		
		Double[] data = BoxPlotCalcUtil.calcPlotBoxValues(list);		
		BoxPlotSerie boxPlotSerie = new BoxPlotSerie();
		boxPlotSerie.getData().add(data);
		return boxPlotSerie;
	}
	
	@Override
	public List<BoxPlotSerie> getBoxPlotSeries(String entityName,
			String attributeName,
			List<QueryRule> queryRules,
			String split)
	{
		Sort sort = new Sort(Sort.DEFAULT_DIRECTION, attributeName);
		Iterable<? extends Entity> iterable = getIterable(entityName, queryRules, sort);
		
		Map<String, List<Double>> boxPlotDataListMap = new HashMap<String, List<Double>>();
		for (Entity entity : iterable)
		{
			String splitValue = split + "__" + entity.get(split);
			if(!boxPlotDataListMap.containsKey(splitValue)){
				boxPlotDataListMap.put(splitValue,  new ArrayList<Double>());
			}
			
			boxPlotDataListMap.get(splitValue).add(entity.getDouble(attributeName));
		}
		
		List<BoxPlotSerie> boxPlotSeries = new ArrayList<BoxPlotSerie>();
		for (Entry<String, List<Double>> entry : boxPlotDataListMap.entrySet()){
			Double[] data = BoxPlotCalcUtil.calcPlotBoxValues(entry.getValue());		
			BoxPlotSerie boxPlotSerie = new BoxPlotSerie();
			boxPlotSerie.getData().add(data);
			boxPlotSerie.setName(entry.getKey());
			boxPlotSeries.add(boxPlotSerie);
		}
		
		return boxPlotSeries;
		
	}
	
	private Iterable<? extends Entity> getIterable(String entityName, List<QueryRule> queryRules, Sort sort){
		Repository<? extends Entity> repo = dataService.getRepositoryByEntityName(entityName);

		if (!(repo instanceof Queryable))
		{
			throw new MolgenisChartException("entity: " + entityName + " is not queryable and is not supported");
		}
			
		final Query q;
		if (queryRules == null)
		{
			q = new QueryImpl();
		}
		else
		{
			q = new QueryImpl(queryRules);
		}

		if (null != sort) {
			q.sort(sort);
		}

		return ((Queryable<? extends Entity>) repo).findAll(q);
	}

	private Object getJavaEntityValue(Entity entity, String attributeName, Class<?> attributeJavaType)
	{	
		if(Double.class == attributeJavaType)
		{
			return entity.getDouble(attributeName);
		} 
		else if (Date.class == attributeJavaType) 
		{
			return entity.getDate(attributeName);
		}
		else if (String.class == attributeJavaType) 
		{
			return entity.getString(attributeName);
		} 
		else if (Timestamp.class == attributeJavaType) 
		{
			return entity.getTimestamp(attributeName);
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataMatrix getDataMatrix(String entityName, List<String> attributeNamesXaxis, String attributeNameYaxis,
			List<QueryRule> queryRules)
	{
		// dataService
		// .registerEntitySource("excel:///Users/erwin/projects/molgenis/molgenis-charts/src/test/resources/heatmap.xlsx");

		Iterable<? extends Entity> iterable = dataService.getRepositoryByEntityName(entityName);

		if (queryRules != null && !queryRules.isEmpty())
		{
			if (!(iterable instanceof Queryable))
			{
				throw new MolgenisChartException("There a query rules defined but the " + entityName
						+ " repository is not queryable");
			}

			QueryImpl q = new QueryImpl();
			for (QueryRule queryRule : queryRules)
			{
				q.addRule(queryRule);
			}

			iterable = ((Queryable<? extends Entity>) iterable).findAll(q);
		}

		List<Target> rowTargets = new ArrayList<Target>();
		List<Target> columnTargets = new ArrayList<Target>();
		List<List<Number>> values = new ArrayList<List<Number>>();

		for (String columnTargetName : attributeNamesXaxis)
		{
			columnTargets.add(new Target(columnTargetName));
		}

		for (Entity entity : iterable)
		{
			String rowTargetName = entity.getString(attributeNameYaxis) != null ? entity.getString(attributeNameYaxis) : "";
			rowTargets.add(new Target(rowTargetName));

			List<Number> rowValues = new ArrayList<Number>();
			for (String attr : attributeNamesXaxis)
			{
				rowValues.add(entity.getDouble(attr));
			}
			values.add(rowValues);
		}

		return new DataMatrix(columnTargets, rowTargets, values);
	}
}
