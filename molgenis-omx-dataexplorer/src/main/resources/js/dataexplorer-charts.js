(function($, molgenis) {
	"use strict";
	molgenis.charts = molgenis.charts || {};
	var ns = molgenis.charts.dataexplorer = molgenis.charts.dataexplorer || {};
	var selectedFeaturesSelectOptions;
	
	ns.createScatterPlotChartRequestPayLoad = function (
			entity,
			x, 
			y, 
			xAxisLabel,
			yAxisLabel,
			width, 
			height, 
			title,
			query,
			splitFeature) {
		
		return {
			"entity" : entity,
			"width": width,
			"height": height,
			"title": title,
			"type": "SCATTER_CHART",
			"query": query,
			"x": x,
			"y": y,
			"xAxisLabel": xAxisLabel,
			"yAxisLabel": yAxisLabel,
			"split": splitFeature
		};
	};
	
	ns.createBoxPlotChartRequestPayLoad = function (
			entity,
			featureIdentifier) {
		
		return {
			"entity" : entity,
			"type" : "BOXPLOT_CHART",
			"observableFeature": featureIdentifier
		};
	};
	
	ns.getSelectedFeaturesSelectOptions = function() {
		var tree = $('#feature-selection').dynatree('getTree');
		var selectedNodes = tree.getSelectedNodes();
		var listItems = [];
		var tempData;
		listItems.push("<option value="+ '-1' +">select</option>");
		$.each(selectedNodes, function (index) {
			tempData = selectedNodes[index].data;
			if(!tempData.isFolder){
				listItems.push("<option value=" + tempData.key + ">" + tempData.title + "</option>");
			}
			tempData = null;
		});
		
		return listItems.join('');
	};
	
	ns.getFeatureByRestApi = function(value,restApi) {
		try
		{
			return restApi.get(value);
		}
		catch (err) 
		{
			console.log(err);
			return undefined;
		}
	};
	
	//Scatter Plot
	ns.makeScatterPlotChartRequest = function (entity, restApi) {
		var xAxisFeature = ns.getFeatureByRestApi($("#scatterplot-select-xaxis-feature").val(), restApi);
		var yAxisFeature = ns.getFeatureByRestApi($("#scatterplot-select-yaxis-feature").val(), restApi);
		var splitFeature = ns.getFeatureByRestApi($("#scatterplot-select-split-feature").val(), restApi);
		var width = 1400;
		var height = 800; 
		var title = $('#scatterplot-title').val();
		var searchRequest = molgenis.createSearchRequest();
		var query = searchRequest.query;
		var x, y, xAxisLabel, yAxisLabel, split;
		
		if(xAxisFeature) {
			x = xAxisFeature.identifier;
			xAxisLabel = xAxisFeature.name;
		} 
		
		if(yAxisFeature) {
			y = yAxisFeature.identifier;
			yAxisLabel = yAxisFeature.name;
		}
		
		if(splitFeature) {
			split = splitFeature.identifier;
		}
		
		$.ajax({
			type : "POST",
			url : "/charts/xydatachart",
			data : JSON.stringify(molgenis.charts.dataexplorer.createScatterPlotChartRequestPayLoad(
					entity,
					x, 
					y, 
					xAxisLabel,
					yAxisLabel,
					width,
					height,
					title,
					query,
					split
			)),
			contentType : "application/json; charset=utf-8",
			cache: false,
			async: true,
			success : function(options){
				console.log(options);
				$('#tabs a:last').tab('show');
			 	$('#chart-container').highcharts(options);
				//$('#chart-container').highcharts("StockChart", options);
			}
		});
		
	};
	
	//Box Plot
	ns.makeBoxPlotChartRequest = function (entity, restApi) {
		var feature = restApi.get($("#boxplot-select-feature").val());
		var featureIdentifier = feature.identifier;
		
		$.ajax({
			type : "POST",
			url : "/charts/boxplot",
			data : JSON.stringify(molgenis.charts.dataexplorer.createBoxPlotChartRequestPayLoad(
					entity,
					featureIdentifier
			)),
			contentType : "application/json; charset=utf-8",
			cache: false,
			async: true,
			success : function(options){
				console.log(options);
				$('#tabs a:last').tab('show');
			 	$('#chart-container').highcharts(options);
			}
		});
		
	};
	
	$(function() {
		$('#chart-designer-modal-scatterplot-button').click(function () {
			selectedFeaturesSelectOptions = null;
			$("#scatterplot-select-xaxis-feature").empty();
			$("#scatterplot-select-yaxis-feature").empty();
			$("#scatterplot-select-split-feature").empty();
			selectedFeaturesSelectOptions = ns.getSelectedFeaturesSelectOptions();
			$("#scatterplot-select-xaxis-feature").append(selectedFeaturesSelectOptions);
			$("#scatterplot-select-yaxis-feature").append(selectedFeaturesSelectOptions);
			$("#scatterplot-select-split-feature").append(selectedFeaturesSelectOptions);
		});
		
		$('#chart-designer-modal-boxplot-button').click(function () {
			selectedFeaturesSelectOptions = null;
			$("#boxplot-select-feature").empty();
			selectedFeaturesSelectOptions = ns.getSelectedFeaturesSelectOptions();
			$("#boxplot-select-feature").append(selectedFeaturesSelectOptions);
		});
	});
	
})($, window.top.molgenis = window.top.molgenis || {});