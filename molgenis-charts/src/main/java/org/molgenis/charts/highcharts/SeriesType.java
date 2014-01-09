package org.molgenis.charts.highcharts;

import org.molgenis.charts.MolgenisSerieType;

public enum SeriesType
{
	SCATTER("scatter"),
	BOXPLOT("boxplot");
	
	private String type;
	
	private SeriesType(String type){
		this.type = type;
	}
	
	public String toString(){
		return this.type;
	}

	public static SeriesType getSeriesType(MolgenisSerieType molgenisSerieType)
	{
		if (MolgenisSerieType.SCATTER.equals(molgenisSerieType))
		{
			return SeriesType.SCATTER;
		}
		else if (MolgenisSerieType.BOXPLOT.equals(molgenisSerieType))
		{
			return SeriesType.BOXPLOT;
		}
		else
		{
			return null;
		}
	}
}
