package org.molgenis.charts.requests;

import javax.validation.constraints.NotNull;

import org.molgenis.charts.Chart;

public abstract class ChartRequest
{
	@NotNull
	private String entity;

	private String title;
	private String xLabel;
	private String yLabel;

	private int width = Chart.DEFAULT_WITH;
	private int height = Chart.DEFAULT_HEIGHT;

	public String getEntity()
	{
		return entity;
	}

	public String getxLabel()
	{
		return xLabel;
	}

	public void setxLabel(String xLabel)
	{
		this.xLabel = xLabel;
	}

	public String getyLabel()
	{
		return yLabel;
	}

	public void setyLabel(String yLabel)
	{
		this.yLabel = yLabel;
	}

	public void setEntity(String entity)
	{
		this.entity = entity;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}
}
