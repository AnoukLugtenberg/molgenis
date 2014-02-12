(function($, molgenis) {
	"use strict";

	var resultsTable = null;
	var pager = null;
	var featureFilters = {};
	var selectedFeatures = [];
	var searchQuery = null;
	var selectedDataSet = null;
	var restApi = new molgenis.RestClient();
	var searchApi = new molgenis.SearchClient();
	var aggregateView = false;

	molgenis.getSelectedDataSetIdentifier = function() {
		return selectedDataSet.name;
	};

	molgenis.createFeatureSelectionTree = function(entityUri) {
		var entityMetaUri = entityUri + "/meta/tree";
		
		restApi.getAsync(entityMetaUri, null, null, 
			function(entityMetaData) {
				selectedDataSet = entityMetaData;
			
				var container = $('#feature-selection');
				
				// Clean #feature-selection
				if (container.children('ul').length > 0) {
					container.dynatree('destroy');
				}
				container.empty();
				
				// Error message
				if (!entityMetaData) {
					container.append("<p>No features available</p>"); 
					return;
				}
				
				// render tree and open first branch
				container.dynatree({
					checkbox : !aggregateView,
					selectMode : 3,
					minExpandLevel : 2,
					debugLevel : 0,
					children : [{
						key : entityMetaData,
						title : entityMetaData.label,
						icon : false,
						isFolder : true,
						isLazy : true,
						children : createChildren("/api/v1/" + entityMetaData.name + "/meta/tree"
								, {select : true}, {})
					}],
					onLazyRead : function(node) {
						// workaround for dynatree lazy parent node select bug
						var opts = node.isSelected() ? {expand : true, select : true} : {};						
						var children = createChildren(node.data.key, opts, opts);
						node.setLazyNodeStatus(DTNodeStatus_Ok);
						node.addChild(children);
					},
					onClick : function(node, event) {
						// target type null is filter icon
						if ((node.getEventTargetType(event) === "title"
								|| node.getEventTargetType(event) === "icon" 
									|| node.getEventTargetType(event) === null)
							&& !node.data.isFolder)
						{
							molgenis.openFeatureFilterDialogNew(node.data.key);
						}
						
						//Clean the select boxes of the charts designer
						if (molgenis.charts) 
						{
							molgenis.charts.dataexplorer.resetChartDesigners();
						}
					},
					onSelect : function(select, node) {
						// workaround for dynatree lazy parent node select bug
						if (select) 
						{
							expandNodeRec(node);
						}
						onNodeSelectionChangeNew(this.getSelectedNodes());
					},
					onPostInit : function() {
						onNodeSelectionChangeNew(this.getSelectedNodes());
					}
				});
			}
		);
					
		function createChildren(href, featureOpts, protocolOpts) {
			var children = [];
			var attributes = restApi.get(href).attributes;
			
			Object.keys(attributes).map(function(prop) {	
				// HAS Attributes
				if(attributes[prop].fieldType === "HAS"){
					children.push($.extend({
						key : attributes[prop].refEntity["href"] + "/tree",
						title : attributes[prop].label,
						tooltip : attributes[prop].description,
						isFolder : true,
						isLazy : protocolOpts.expand != true,
						children : protocolOpts.expand ? createChildren(
								attributes[prop].refEntity["href"] + "/tree", featureOpts, protocolOpts) : null
					}, protocolOpts));
				}
				else {
					children.push($.extend({
						key : attributes[prop].refThis,
						title : attributes[prop].label,
						tooltip : attributes[prop].description,
						icon : "../../img/filter-bw.png",
						isFolder : false
					}, featureOpts));
				}
			});

		
			return children;		
		}
		
		function expandNodeRec(node) {
			if (node.childList == undefined) {
				node.toggleExpand();
			} else {
				$.each(node.childList, function() {
					expandNodeRec(this);
				});
			}
		}
		
		function onNodeSelectionChangeNew(selectedNodes) {
			function getSiblingPos(node) {
				var pos = 0;
				do {
					node = node.getPrevSibling();
					if (node == null)
						break;
					else
						++pos;
				} while (true);
				return pos;
			}
			
			var sortedNodes = selectedNodes.sort(function(node1, node2) {
				var diff = node1.getLevel() - node2.getLevel();
				if (diff == 0) {
					diff = getSiblingPos(node1.getParent())
							- getSiblingPos(node2.getParent());
					if (diff == 0)
						diff = getSiblingPos(node1) - getSiblingPos(node2);
				}
				return diff <= 0 ? -1 : 1;
			});
			
			var sortedFeatures = $.map(sortedNodes, function(node) {
				return node.data.isFolder || node.data.key == 'more'
						? null
						: node.data.key;
			});

			molgenis.onFeatureSelectionChangeNew(sortedFeatures);

		}
	};
	
	molgenis.openFeatureFilterDialogNew = function(attributeUri) {
		restApi.getAsync(attributeUri, null, null,
			function(feature) {
				var items = [];
				if (feature.description) {
					items.push('<h3>Description</h3><p>' + feature.description + '</p>');
				}
				items.push('<h3>Filter:</h3>');
				var config = featureFilters[attributeUri];
				var applyButton = $('<input type="button" class="btn pull-left" value="Apply filter">');
				var divContainer = molgenis.createGenericFeatureFieldNew(items, feature, config, applyButton, attributeUri, false);
				
				molgenis.createSpecificFeatureField(items, divContainer, feature, config, applyButton, attributeUri);
			}
		);
	};

	molgenis.onDataSetSelectionChange = function(entityUri) {
		// reset
		featureFilters = {};
		$('#feature-filters p').remove();
		selectedFeatures = [];
		searchQuery = null;
		resultsTable.resetSortRule();
		$("#observationset-search").val("");
		$('#data-table-pager').empty();
		pager = null;
		molgenis.createFeatureSelectionTree(entityUri);
	};

	molgenis.onFeatureSelectionChangeNew = function(attributeUris) {
		selectedFeatures = attributeUris;
		molgenis.updateObservationSetsTable();
	};

	molgenis.searchObservationSets = function(query) {
		// Reset
		resultsTable.resetSortRule();

		searchQuery = query;
		molgenis.updateObservationSetsTable();
	};

	molgenis.updateObservationSetsTable = function() {
		if (selectedFeatures.length > 0) {
			molgenis.search(function(searchResponse) {
				var maxRowsPerPage = resultsTable.getMaxRows();
				var nrRows = searchResponse.totalHitCount;
				resultsTable.build(searchResponse, selectedFeatures, restApi);
				molgenis.onObservationSetsTableChange(nrRows, maxRowsPerPage);
			});
		} else {
			$('#nrOfDataItems').html('no features selected');
			$('#data-table-pager').empty();
			$('#data-table').empty();
		}
	};

	molgenis.updateObservationSetsTablePage = function() {
		if (selectedFeatures.length > 0) {
			molgenis.search(function(searchResponse) {
				console.log("updateObservationSetsTablePage");
				resultsTable.build(searchResponse, selectedFeatures, restApi);
			});
		} else {
			$('#nrOfDataItems').html('no features selected');
			$('#data-table-pager').empty();
			$('#data-table').empty();
		}
	};

	molgenis.onObservationSetsTableChange = function(nrRows, maxRowsPerPage) {		
		molgenis.updateObservationSetsTablePager(nrRows, maxRowsPerPage);
		molgenis.updateObservationSetsTableHeader(nrRows);
	};

	molgenis.updateObservationSetsTableHeader = function(nrRows) {
		if (nrRows == 1)
			$('#nrOfDataItems').html(nrRows + ' data item found');
		else
			$('#nrOfDataItems').html(nrRows + ' data items found');
	};

	molgenis.updateObservationSetsTablePager = function(nrRows, nrRowsPerPage) {
		$('#data-table-pager').pager({
			'nrItems' : nrRows,
			'nrItemsPerPage' : nrRowsPerPage,
			'onPageChange' : molgenis.updateObservationSetsTablePage
		});
		pager = $('#data-table-pager');
	};

	molgenis.openFeatureFilterDialog = function(featureUri) {
		restApi
				.getAsync(
						featureUri,
						null,
						null,
						function(feature) {

							var items = [];
							if (feature.description)
								items.push('<h3>Description</h3><p>'
										+ feature.description + '</p>');
							items.push('<h3>Filter:</h3>');
							var config = featureFilters[featureUri];
							var applyButton = $('<input type="button" class="btn pull-left" value="Apply filter">');

							var divContainer = molgenis
									.createGenericFeatureField(items, feature,
											config, applyButton, featureUri,
											false);
							molgenis.createSpecificFeatureField(items,
									divContainer, feature, config, applyButton,
									featureUri);
						});

	};

	//Filter dialog for one feature
	molgenis.createSpecificFeatureField = function(items, divContainer,
			attribute, config, applyButton, featureUri) {
		switch (attribute.fieldType) {
			case "HTML" :
			case "MREF" :
			case "XREF" :
			case "EMAIL" :
			case "HYPERLINK" :
			case "TEXT" :
			case "STRING" :
				if (divContainer.find(
                    $("[id='text_" + attribute.name +"']")).val() === "") {
					$(applyButton).prop('disabled', true);
				}
				divContainer.find(
                    $("[id='text_" + attribute.name + "']")).keyup(
                        function (e) {
                            if (divContainer.find(
                                $("[id='text_" + attribute.name + "']")
                                    .val() == "")) {
                                $(applyButton).prop('disabled', true);
                            } else {
                                $(applyButton).prop('disabled', false);
                            }
                        });

				applyButton.click(function() {
					molgenis.updateFeatureFilter(featureUri, {
						name : attribute.label,
						identifier : attribute.name,
						type : attribute.fieldType,
						values : [$("[id='text_" + attribute.name + "']")
								.val()]
					});
					$('.feature-filter-dialog').dialog('close');
				});

				break;

			case "DATE" :
			case "DATE_TIME" :
				var datePickerFrom = $('<div id="from" class="input-append date" />');
				var filterFrom;

				if (config == null) {
					filterFrom = datePickerFrom
							.append($('<input id="date-feature-from"  type="text"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
					applyButton.attr('disabled', 'disabled');
				} else {
					filterFrom = datePickerFrom
							.append($('<input id="date-feature-from"  type="text" value="'
									+ config.values[0].replace("T", "'T'")
									+ '"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				}

				datePickerFrom.on('changeDate', function(e) {
					$('#date-feature-to').val($('#date-feature-from').val());
					applyButton.removeAttr('disabled');
				});

				var datePickerTo = $('<div id="to" class="input-append date" />');
				var filterTo;

				if (config == null)
					filterTo = datePickerTo
							.append($('<input id="date-feature-to" type="text"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				else
					filterTo = datePickerTo
							.append($('<input id="date-feature-to" type="text" value="'
									+ config.values[1].replace("T", "'T'")
									+ '"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));

				var filter = $('<span>From:<span>').after(filterFrom).after(
						$('<span>To:</span>')).after(filterTo);
				$(".feature-filter-dialog").dialog("option", "width", 710);

				datePickerTo.on('changeDate', function(e) {
					applyButton.removeAttr('disabled');
				});

				divContainer.empty().append(filter);

				applyButton.click(function() {
					molgenis.updateFeatureFilter(featureUri,
							{
								name : attribute.label,
								identifier : attribute.name,
								type : attribute.fieldType,
								range : true,
								values : [
										$('#date-feature-from').val().replace(
												"'T'", "T"),
										$('#date-feature-to').val().replace(
												"'T'", "T")]
							});
					$('.feature-filter-dialog').dialog('close');
				});

				break;
			case "LONG" :
			case "INT" :
			case "DECIMAL" :
				applyButton.click(function() {
					molgenis.updateFeatureFilter(featureUri, {
						name : attribute.label,
						identifier : attribute.name,
						type : attribute.fieldType,
						values : [
								$("[id='from_" + attribute.name+"']")
										.val(),
								$("[id='to_" + attribute.name+"']")
										.val()],
						range : true
					});
					$('.feature-filter-dialog').dialog('close');
				});
				break;
			case "BOOL" :
				$(applyButton).prop('disabled', true);
				$('input[type=radio]').live('change', function() {
					$(applyButton).prop('disabled', false);
				});
				applyButton.click(function() {
					molgenis.updateFeatureFilter(featureUri, {
						name : attribute.label,
						identifier : attribute.name,
						type : attribute.fieldType,
						values : [$(
								'input[name=bool-feature_' + attribute.name
										+ ']:checked').val()]
					});
					$('.feature-filter-dialog').dialog('close');
				});
				break;
			case "CATEGORICAL" :
				if (config && config.values.length > 0) {
					$(applyButton).prop('disabled', false);
				} else {
					$(applyButton).prop('disabled', true);
				}
				$('.cat-value').live('change', function() {
					if ($('.cat-value:checked').length > 0) {
						applyButton.removeAttr('disabled');
					} else {
						$(applyButton).prop('disabled', true);
					}
				});
	
				applyButton.click(function() {
					molgenis.updateFeatureFilter(featureUri, {
						name : attribute.label,
						identifier : attribute.name,
						type : attribute.fieldType,
						values : $.makeArray($('.cat-value:checked').map(
								function() {
									return $(this).val();
								}))
					});
					$('.feature-filter-dialog').dialog('close');
				});
				break;
			default :
				return;
	
		}
		
		var applyButtonHolder = $('<div id="applybutton-holder" />').append(
				applyButton);
		if ($.isArray(divContainer)) {
			divContainer.push(applyButtonHolder);
		} else {
			divContainer.after(applyButtonHolder);
		}
		$('.feature-filter-dialog').html(items.join('')).append(divContainer);
	
		$('.feature-filter-dialog').dialog({
			title : attribute.label,
			dialogClass : 'ui-dialog-shadow'
		});
		$('.feature-filter-dialog').dialog('open');
	
		$('.feature-filter-dialog').keyup(function(e) {
			if (e.keyCode == 13) // enter
			{
				if (applyButton.attr("disabled") != "disabled") {//enter only works if button is enabled (filter input is given)
					applyButton.click();
				}
			}
			if (e.keyCode == 27)// esc
			{
				$('.feature-filter-dialog').dialog('close');
			}
		});
	
		if (attribute.fieldType === 'DATE') {
			$('.date').datetimepicker({
				format : 'yyyy-MM-dd',
				language : 'en',
				pickTime : false
			});
		} else if (attribute.fieldType === 'DATE_TIME') {
			$('.date').datetimepicker({
				format : "yyyy-MM-dd'T'hh:mm:ss" + getCurrentTimezoneOffset(),
				language : 'en',
				pickTime : true
			});
		}
	};

	//Generic part for filter fields
	molgenis.createGenericFeatureFieldNew = function(items, attribute, config, applyButton, featureUri, wizard) {
		var divContainer = $('<div />');
		var filter = null;
		
		switch (attribute.fieldType) {
			case "HTML" :
			case "MREF" :
			case "XREF" :
			case "EMAIL" :
			case "HYPERLINK" :
			case "TEXT" :
			case "STRING" :
				if (config == null) {
					filter = $('<input type="text" id="text_'
							+ attribute.name + '">');
				} else {
	
					filter = $('<input type="text" id="text_'
							+ attribute.name
							+ '" placeholder="filter text" autofocus="autofocus" value="'
							+ config.values[0] + '">');
				}
				divContainer.append(filter);
				if (wizard) {
	              $("[id='text_" + attribute.name + "']").change(
							function() {
								molgenis.updateFeatureFilter(featureUri, {
									name : attribute.label,
									identifier : attribute.name,
									type : attribute.fieldType,
									values : [$(this).val()]
								});
	
							});
				}
				break;
			case "DATE" :
			case "DATE_TIME" :
				var datePickerFrom = $('<div id="from_' + attribute.name
						+ '" class="input-append date" />');
				var filterFrom;
	
				if (config == null) {
					filterFrom = datePickerFrom.append($('<input id="date-feature-from_'
									+ attribute.name
									+ '"  type="text" /><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				} else {
					filterFrom = datePickerFrom.append($('<input id="date-feature-from_'
									+ attribute.name
									+ '"  type="text" value="'
									+ config.values[0].replace("T", "'T'")
									+ '" /><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				}
	
				datePickerFrom.on('changeDate', function(e) {
	              $("[id='date-feature-to_" + attribute.name+"']").val(
	              		$("[id='date-feature-from_" + attribute.name+"']").val()
	              );
				});
	
				var datePickerTo = $('<div id="to_' + attribute.name
						+ '" class="input-append date" />');
				var filterTo;
	
				if (config == null)
					filterTo = datePickerTo
							.append($('<input id="date-feature-to_'
									+ attribute.name
									+ '" type="text"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				else
					filterTo = datePickerTo
							.append($('<input id="date-feature-to_'
									+ attribute.name
									+ '" type="text" value="'
									+ config.values[1].replace("T", "'T'")
									+ '"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
	
				filter = $('<span>From:<span>').after(filterFrom).after(
						$('<span>To:</span>')).after(filterTo);
				divContainer.append(filter);
				break;
			case "LONG" :
			case "INT" :
			case "DECIMAL" :
				var fromFilter;
				var toFilter;
	
				if (config == null) {
					fromFilter = $('<input id="from_' + attribute.name
							+ '" type="number" step="any" style="width:100px">');
					toFilter = $('<input id="to_' + attribute.name
							+ '" type="number" step="any" style="width:100px">');
				} else {
					fromFilter = $('<input id="from_'
							+ attribute.name
							+ '" type="number" step="any" style="width:100px" value="'
							+ config.values[0] + '">');
					toFilter = $('<input id="to_'
							+ attribute.name
							+ '" type="number" step="any" style="width:100px" value="'
							+ config.values[1] + '">');
				}
	
				fromFilter.on('keyup input', function() {
					// If 'from' changed set 'to' at the same value
					$("[id='to_" + attribute.name+"']").val(
							$("[id='from_"
													+ attribute.name + '')
									.val());
				});
	
				filter = $('<span>From:<span>').after(fromFilter).after(
						$('<span>To:</span>')).after(toFilter);
	
				divContainer.append(filter);
				if (wizard) {
					divContainer
							.find(
	                      $("[id='from_" + attribute.name +"']"))
							.change(
									function() {
	
										molgenis
												.updateFeatureFilter(
														featureUri,
														{
															name : attribute.label,
															identifier : attribute.name,
															type : attribute.fieldType,
															values : [
																	$(
	                                                                  $("[id='from_"
																							+ attribute.name + "']"))
																			.val(),
																	$(
	                                                                  $("[id='to_"
	
																							+ attribute.name + "']"))
																			.val()],
															range : true
														});
	
									});
					divContainer
							.find($("[id='to_" + attribute.name + "']"))
							.change(
									function() {
										molgenis
												.updateFeatureFilter(
														featureUri,
														{
															name : attribute.label,
															identifier : attribute.name,
															type : attribute.fieldType,
															values : [
																	$(
	                                                                  $("[id='from_"
																							+ attribute.name +"']"))
																			.val(),
																	$(
	                                                                  $("[id='to_"
	
																							+ attribute.name + "']"))
																			.val()],
															range : true
														});
									});
				}
				break;
			case "BOOL" :
				if (config == null) {
					filter = $('<label class="radio"><input type="radio" id="bool-feature-true_'
							+ attribute.name
							+ '" name="bool-feature_'
							+ attribute.name
							+ '" value="true">True</label><label class="radio"><input type="radio" id="bool-feature-fl_'
							+ attribute.name
							+ '" name="bool-feature_'
							+ attribute.name
							+ '" value="false">False</label>');
	
				} else {
					if (config.values[0] == 'true') {
						filter = $('<label class="radio"><input type="radio" id="bool-feature-true_'
								+ attribute.name
								+ '" name="bool-feature_'
								+ attribute.name
								+ '" checked value="true">True</label><label class="radio"><input type="radio" id="bool-feature-fl_'
								+ attribute.name
								+ '" name="bool-feature_'
								+ attribute.name
								+ '" value="false">False</label>');
					} else {
						filter = $('<label class="radio"><input type="radio" id="bool-feature-true_'
								+ attribute.name
								+ '" name="bool-feature_'
								+ attribute.name
								+ '" value="true">True</label><label class="radio"><input type="radio" id="bool-feature-fl_'
								+ attribute.name
								+ '" name="bool-feature_'
								+ attribute.name
								+ '" checked value="false">False</label>');
					}
				}
				divContainer.append(filter);
				if (wizard) {
					filter.find(
							'input[name="bool-feature_' + attribute.name
									+ '"]').change(function() {
						molgenis.updateFeatureFilter(featureUri, {
							name : attribute.label,
							identifier : attribute.name,
							type : attribute.fieldType,
							values : [$(this).val()]
						});
	
					});
				}
	
				break;
			case "CATEGORICAL" :
				$.ajax({
					type : 'POST',
					url : '/api/v1/category?_method=GET',
					data : JSON.stringify({
						q : [{
							"field" : "observableFeature_Identifier",
							"operator" : "EQUALS",
							"value" : attribute.name
						}]
					}),
					contentType : 'application/json',
					async : false,
					success : function(categories) {
						filter = [];
						$.each(categories.items,
								function() {
									var input;
									if (config
											&& ($.inArray(this.name,
													config.values) > -1)) {
										input = $('<input type="checkbox" id="'
												+ this.name + '_'
												+ attribute.name
												+ '" "class="cat-value" name="'
												+ attribute.name
												+ '" value="' + this.name
												+ '"checked>');
									} else {
										input = $('<input type="checkbox" id="'
												+ this.name + '_'
												+ attribute.name
												+ '" class="cat-value" name="'
												+ attribute.name
												+ '" value="' + this.name
												+ '">');
									}
									filter.push($('<label class="checkbox">')
											.html(' ' + this.name).prepend(
													input));
								});
					}
				});
				divContainer.append(filter);
				if (wizard) {
					divContainer.find(
							'input[name="' + attribute.name + '"]').click(
							function() {
								molgenis.updateFeatureFilter(featureUri, {
									name : attribute.label,
									identifier : attribute.name,
									type : attribute.fieldType,
									values : $.makeArray($(
											'input[name="' + attribute.name
													+ '"]:checked').map(
											function() {
												return $(this).val();
											}))
								});
							});
				}
				break;
			default :
				return;
		}
	
		if ((attribute.fieldType === 'XREF') || (attribute.fieldType == 'MREF')) {
			// FIXME get working for generic data explorer
			divContainer
					.find($("[id='text_" + attribute.name +"']"))
					.autocomplete(
							{
								source : function(request, response) {
									$
											.ajax({
												type : 'POST',
												url : '/api/v1/characteristic?_method=GET',
												data : JSON.stringify({
													num : 15,
													q : [{
														"field" : "name",
														"operator" : "LIKE",
														"value" : request.term
													}]
												}),
												contentType : 'application/json',
												async : true,
												success : function(
														characteristicList) {
													response($
															.map(
																	characteristicList.items,
																	function(
																			item) {
																		return item.name;
																	}));
												}
											});
								},
								minLength : 2
							});
		}
	
		if (attribute.fieldType === 'DATE') {
			var container = divContainer.find('.date').datetimepicker({
				format : 'yyyy-MM-dd',
				language : 'en',
				pickTime : false
			});
	
			if (wizard) {
				container
						.on(
								'changeDate',
								function(e) {
									molgenis
											.updateFeatureFilter(
													featureUri,
													{
														name : attribute.label,
														identifier : attribute.name,
														type : attribute.fieldType,
														range : true,
														values : [
																    $("[id='date-feature-from_"+ attribute.name
	                                                                    +"']")
																		.val(),
	                                                            $("[id='date-feature-to_"
																				+ attribute.name
	                                                                        +"']")
																		.val()]
													});
								});
			}
		} else if (attribute.fieldType === 'DATE_TIME') {
			// FIXME get working for generic data explorer
			var container = divContainer.find('.date').datetimepicker({
				format : "yyyy-MM-dd'T'hh:mm:ss" + getCurrentTimezoneOffset(),
				language : 'en',
				pickTime : true
			});
			if (wizard) {
				container
						.on(
								'changeDate',
								function(e) {
									molgenis
											.updateFeatureFilter(
													featureUri,
													{
														name : attribute.label,
														identifier : attribute.name,
														type : attribute.fieldType,
														range : true,
														values : [
																$("[id='date-feature-from_" + attribute.name + "']")
	
																		.val()
																		.replace(
																				"'T'",
																				"T"),
																$("[id='date-feature-to_"+ attribute.name + "']")
																		.val()
																		.replace(
																				"'T'",
																				"T")]
													});
								});
			}
		}
	
		return divContainer;
	};

	//Generic part for filter fields
	molgenis.createGenericFeatureField = function(items, feature, config,
			applyButton, featureUri, wizard) {
		var divContainer = $('<div />');
		var filter = null;

		switch (feature.dataType) {
			case "html" :
			case "mref" :
			case "xref" :
			case "email" :
			case "hyperlink" :
			case "text" :
			case "string" :
				if (config == null) {
					filter = $('<input type="text" id="text_'
							+ feature.identifier + '">');
				} else {

					filter = $('<input type="text" id="text_'
							+ feature.identifier
							+ '" placeholder="filter text" autofocus="autofocus" value="'
							+ config.values[0] + '">');
				}
				divContainer.append(filter);
				if (wizard) {
                    $("[id='text_" + feature.identifier+"']").change(
							function() {
								molgenis.updateFeatureFilter(featureUri, {
									name : feature.name,
									identifier : feature.identifier,
									type : feature.dataType,
									values : [$(this).val()]
								});

							});
				}
				break;
			case "date" :
			case "datetime" :
				var datePickerFrom = $('<div id="from_' + feature.identifier
						+ '" class="input-append date" />');
				var filterFrom;

				if (config == null) {
					filterFrom = datePickerFrom
							.append($('<input id="date-feature-from_'
									+ feature.identifier
									+ '"  type="text" /><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				} else {
					filterFrom = datePickerFrom
							.append($('<input id="date-feature-from_'
									+ feature.identifier
									+ '"  type="text" value="'
									+ config.values[0].replace("T", "'T'")
									+ '" /><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				}

				datePickerFrom.on('changeDate', function(e) {
                    $("[id='date-feature-to_"
											+ feature.identifier+"']").val(
                            $("[id='date-feature-from_"
													+ feature.identifier+"']")
									.val());
				});

				var datePickerTo = $('<div id="to_' + feature.identifier
						+ '" class="input-append date" />');
				var filterTo;

				if (config == null)
					filterTo = datePickerTo
							.append($('<input id="date-feature-to_'
									+ feature.identifier
									+ '" type="text"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));
				else
					filterTo = datePickerTo
							.append($('<input id="date-feature-to_'
									+ feature.identifier
									+ '" type="text" value="'
									+ config.values[1].replace("T", "'T'")
									+ '"><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span>'));

				filter = $('<span>From:<span>').after(filterFrom).after(
						$('<span>To:</span>')).after(filterTo);
				divContainer.append(filter);
				break;
			case "long" :
			case "integer" :
			case "int" :
			case "decimal" :
				var fromFilter;
				var toFilter;

				if (config == null) {
					fromFilter = $('<input id="from_' + feature.identifier
							+ '" type="number" step="any" style="width:100px">');
					toFilter = $('<input id="to_' + feature.identifier
							+ '" type="number" step="any" style="width:100px">');
				} else {
					fromFilter = $('<input id="from_'
							+ feature.identifier
							+ '" type="number" step="any" style="width:100px" value="'
							+ config.values[0] + '">');
					toFilter = $('<input id="to_'
							+ feature.identifier
							+ '" type="number" step="any" style="width:100px" value="'
							+ config.values[1] + '">');
				}

				fromFilter.on('keyup input', function() {
					// If 'from' changed set 'to' at the same value
					$("[id='to_" + feature.identifier+"']").val(
							$("[id='from_"
													+ feature.identifier + '')
									.val());
				});

				filter = $('<span>From:<span>').after(fromFilter).after(
						$('<span>To:</span>')).after(toFilter);

				divContainer.append(filter);
				if (wizard) {
					divContainer
							.find(
                            $("[id='from_"
													+ feature.identifier+"']"))
							.change(
									function() {

										molgenis
												.updateFeatureFilter(
														featureUri,
														{
															name : feature.name,
															identifier : feature.identifier,
															type : feature.dataType,
															values : [
																	$(
                                                                        $("[id='from_"
																							+ feature.identifier+"']"))
																			.val(),
																	$(
                                                                        $("[id='to_"
																							+ feature.identifier+"']"))
																			.val()],
															range : true
														});

									});
					divContainer
							.find($("[id='to_" + feature.identifier+"']"))
							.change(
									function() {
										molgenis
												.updateFeatureFilter(
														featureUri,
														{
															name : feature.name,
															identifier : feature.identifier,
															type : feature.dataType,
															values : [
																	$(
                                                                        $("[id='from_"
																							+ feature.identifier+"']"))
																			.val(),
																	$(
                                                                        $("[id='to_"
																							+ feature.identifier+"']"))
																			.val()],
															range : true
														});
									});
				}
				break;
			case "bool" :
				if (config == null) {
					filter = $('<label class="radio"><input type="radio" id="bool-feature-true_'
							+ feature.identifier
							+ '" name="bool-feature_'
							+ feature.identifier
							+ '" value="true">True</label><label class="radio"><input type="radio" id="bool-feature-fl_'
							+ feature.identifier
							+ '" name="bool-feature_'
							+ feature.identifier
							+ '" value="false">False</label>');

				} else {
					if (config.values[0] == 'true') {
						filter = $('<label class="radio"><input type="radio" id="bool-feature-true_'
								+ feature.identifier
								+ '" name="bool-feature_'
								+ feature.identifier
								+ '" checked value="true">True</label><label class="radio"><input type="radio" id="bool-feature-fl_'
								+ feature.identifier
								+ '" name="bool-feature_'
								+ feature.identifier
								+ '" value="false">False</label>');
					} else {
						filter = $('<label class="radio"><input type="radio" id="bool-feature-true_'
								+ feature.identifier
								+ '" name="bool-feature_'
								+ feature.identifier
								+ '" value="true">True</label><label class="radio"><input type="radio" id="bool-feature-fl_'
								+ feature.identifier
								+ '" name="bool-feature_'
								+ feature.identifier
								+ '" checked value="false">False</label>');
					}
				}
				divContainer.append(filter);
				if (wizard) {
					filter.find(
							'input[name="bool-feature_' + feature.identifier
									+ '"]').change(function() {
						molgenis.updateFeatureFilter(featureUri, {
							name : feature.name,
							identifier : feature.identifier,
							type : feature.dataType,
							values : [$(this).val()]
						});

					});
				}

				break;
			case "categorical" :
				$.ajax({
					type : 'POST',
					url : '/api/v1/category?_method=GET',
					data : JSON.stringify({
						q : [{
							"field" : "observableFeature_Identifier",
							"operator" : "EQUALS",
							"value" : feature.identifier
						}]
					}),
					contentType : 'application/json',
					async : false,
					success : function(categories) {
						filter = [];
						$.each(categories.items,
								function() {
									var input;
									if (config
											&& ($.inArray(this.name,
													config.values) > -1)) {
										input = $('<input type="checkbox" id="'
												+ this.name + '_'
												+ feature.identifier
												+ '" "class="cat-value" name="'
												+ feature.identifier
												+ '" value="' + this.name
												+ '"checked>');
									} else {
										input = $('<input type="checkbox" id="'
												+ this.name + '_'
												+ feature.identifier
												+ '" class="cat-value" name="'
												+ feature.identifier
												+ '" value="' + this.name
												+ '">');
									}
									filter.push($('<label class="checkbox">')
											.html(' ' + this.name).prepend(
													input));
								});
					}
				});
				divContainer.append(filter);
				if (wizard) {
					divContainer.find(
							'input[name="' + feature.identifier + '"]').click(
							function() {
								molgenis.updateFeatureFilter(featureUri, {
									name : feature.name,
									identifier : feature.identifier,
									type : feature.dataType,
									values : $.makeArray($(
											'input[name="' + feature.identifier
													+ '"]:checked').map(
											function() {
												return $(this).val();
											}))
								});
							});
				}
				break;
			default :
				return;
		}

		if ((feature.dataType == 'xref') || (feature.dataType == 'mref')) {
			divContainer
					.find($("[id='text_" + feature.identifier+"']"))
					.autocomplete(
							{
								source : function(request, response) {
									$
											.ajax({
												type : 'POST',
												url : '/api/v1/characteristic?_method=GET',
												data : JSON.stringify({
													num : 15,
													q : [{
														"field" : "name",
														"operator" : "LIKE",
														"value" : request.term
													}]
												}),
												contentType : 'application/json',
												async : true,
												success : function(
														characteristicList) {
													response($
															.map(
																	characteristicList.items,
																	function(
																			item) {
																		return item.name;
																	}));
												}
											});
								},
								minLength : 2
							});
		}

		if (feature.dataType == 'date') {
			var container = divContainer.find('.date').datetimepicker({
				format : 'yyyy-MM-dd',
				language : 'en',
				pickTime : false
			});

			if (wizard) {
				container
						.on(
								'changeDate',
								function(e) {
									molgenis
											.updateFeatureFilter(
													featureUri,
													{
														name : feature.name,
														identifier : feature.identifier,
														type : feature.dataType,
														range : true,
														values : [
																    $("[id='date-feature-from_"+ feature.identifier
                                                                        +"']")
																		.val(),
                                                                $("[id='date-feature-to_"
																				+ feature.identifier
                                                                            +"']")
																		.val()]
													});
								});
			}
		} else if (feature.dataType == 'datetime') {
			var container = divContainer.find('.date').datetimepicker({
				format : "yyyy-MM-dd'T'hh:mm:ss" + getCurrentTimezoneOffset(),
				language : 'en',
				pickTime : true
			});
			if (wizard) {
				container
						.on(
								'changeDate',
								function(e) {
									molgenis
											.updateFeatureFilter(
													featureUri,
													{
														name : feature.name,
														identifier : feature.identifier,
														type : feature.dataType,
														range : true,
														values : [
																$("[id='date-feature-from_"+feature.identifier+"']")

																		.val()
																		.replace(
																				"'T'",
																				"T"),
																$("[id='date-feature-to_"+ feature.identifier+"']")
																		.val()
																		.replace(
																				"'T'",
																				"T")]
													});
								});
			}
		}

		return divContainer;
	};

	molgenis.createFeatureFilterField = function(elements) {
		var attributes = {};
		$.each(elements, function(index, element) {
			var attributeUri = $(element).attr('data-molgenis-url');
			attributes[attributeUri] = restApi.get(attributeUri);
		});
		
		$.each(elements, function(index, element) {
			var attributeUri = $(element).attr('data-molgenis-url');
			
			var config = featureFilters[attributeUri];
			var applyButton = $('<input type="button" class="btn pull-left" value="Apply filter">');
			var divContainer = molgenis
					.createGenericFeatureFieldNew(null, attributes[attributeUri],
							config, applyButton, attributeUri,
							true);
			var trElement = $(element).closest('tr');
			trElement.append(divContainer);
		});
	};

	molgenis.updateFeatureFilter = function(featureUri, featureFilter) {
		var start = molgenis.getFeatureByIdentifier('start_nucleotide');
		var chromosome = molgenis.getFeatureByIdentifier('chromosome');
		if (start != undefined && chromosome != undefined) {
			if (featureUri == start.href) {
				dalliance.setLocation(dalliance.chr, featureFilter.values[0],
						featureFilter.values[1]);
			} else if (featureUri == chromosome.href) {
				dalliance.setLocation(featureFilter.values[0],
						dalliance.viewStart, dalliance.viewEnd);
			}
		}
		featureFilters[featureUri] = featureFilter;
		molgenis.onFeatureFilterChange(featureFilters);
	};

	molgenis.removeFeatureFilter = function(featureUri) {
		delete featureFilters[featureUri];
		molgenis.onFeatureFilterChange(featureFilters);
	};

	molgenis.onFeatureFilterChange = function(featureFilters) {
		molgenis.createFeatureFilterList(featureFilters);
		molgenis.updateObservationSetsTable();
		if ($('#selectFeature').val() != null) {
			molgenis.loadAggregate($('#selectFeature').val());
		}
	};

	molgenis.createFeatureFilterList = function(featureFilters) {
		var items = [];
		$
				.each(
						featureFilters,
						function(featureUri, feature) {
							items
									.push('<p><a class="feature-filter-edit" data-href="'
											+ featureUri
											+ '" href="#">'
											+ feature.name
											+ ' ('
											+ feature.values.join(',')
											+ ')</a><a class="feature-filter-remove" data-href="'
											+ featureUri
											+ '" href="#" title="Remove '
											+ feature.name
											+ ' filter" ><i class="ui-icon ui-icon-closethick"></i></a></p>');
						});
		items.push('</div>');
		$('#feature-filters').html(items.join(''));

		$('.feature-filter-edit').click(function() {
			molgenis.openFeatureFilterDialogNew($(this).data('href'));
			return false;
		});
		$('.feature-filter-remove').click(function() {
			molgenis.removeFeatureFilter($(this).data('href'));
			return false;
		});
	};

	molgenis.search = function(callback) {
		searchApi.search(molgenis.createSearchRequest(true), callback);
	};

	molgenis.createSearchRequest = function(includeLimitOffset) {
		
		var searchRequest = {
			documentType : selectedDataSet.name,
			query : {
				rules : [[]]
			}
		};

		if (includeLimitOffset) {
			searchRequest.query.pageSize = resultsTable.getMaxRows();

			if (pager != null) {
				var page = $('#data-table-pager').pager('getPage');
				if (page > 1) {
					var offset = (page - 1) * resultsTable.getMaxRows();
					searchRequest.query.offset = offset;
				}
			}
		}

		var count = 0;

		if (searchQuery) {
			if (/\S/.test(searchQuery)) {
				searchRequest.query.rules[0].push({
					operator : 'SEARCH',
					value : searchQuery
				});
				count++;
			}
		}

		$.each(featureFilters, function(featureUri, filter) {
			if (count > 0) {
				searchRequest.query.rules[0].push({
					operator : 'AND'
				});
			}
			$.each(filter.values, function(index, value) {
				if (filter.range) {

					// Range filter
					var rangeAnd = false;
					if ((index == 0) && (value != '')) {
						searchRequest.query.rules[0].push({
							field : filter.identifier,
							operator : 'GREATER_EQUAL',
							value : value
						});
						rangeAnd = true;
					}
					if (rangeAnd) {
						searchRequest.query.rules[0].push({
							operator : 'AND'
						});
					}
					if ((index == 1) && (value != '')) {
						searchRequest.query.rules[0].push({
							field : filter.identifier,
							operator : 'LESS_EQUAL',
							value : value
						});
					}

				} else {
					if (index > 0) {
						searchRequest.query.rules[0].push({
							operator : 'OR'
						});
					}
					searchRequest.query.rules[0].push({
						field : filter.identifier,
						operator : 'EQUALS',
						value : value
					});
				}

			});

			count++;
		});

		var sortRule = resultsTable.getSortRule();
		if (sortRule) {
			searchRequest.query.sort = sortRule;
		}
		
		searchRequest.fieldsToReturn = [];
		$.each(selectedFeatures, function() {
			var feature = restApi.get(this);
			searchRequest.fieldsToReturn.push(feature.name);
			if (feature.dataType === 'xref' || feature.dataType === 'mref')
				searchRequest.fieldsToReturn.push("key-" + feature.name);
		});

		return searchRequest;
	};

	molgenis.initializeAggregate = function(entityUri) {
		var entityMetaUri = entityUri + '/meta';
		restApi.getAsync(entityMetaUri, null, null, function(entityMetaData) {
			var selectTag = $('<select id="selectFeature"/>');
			if(Object.keys(entityMetaData.attributes).length === 0) {
				selectTag.attr('disabled', 'disabled');
			} else {
				$.each(entityMetaData.attributes, function(key, attribute) {
					if(attribute.fieldType === 'BOOL' || attribute.fieldType === 'CATEGORICAL') {
						selectTag.append('<option value="' + entityMetaUri + '/' + key + '">' + attribute.label + '</option>');
					}
				});
				$('#feature-select').empty().append(selectTag);
				molgenis.loadAggregate(selectTag.val());
				selectTag.chosen();
				selectTag.change(function() {
					molgenis.loadAggregate($(this).val());
				});
			}
		});
	};

	molgenis.loadAggregate = function(attributeUri) {
		$.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/aggregate',
			data : JSON.stringify({'attributeUri': attributeUri}),
			contentType : 'application/json',
			success : function(aggregateResult) {
				var table = $('<table />').addClass('table table-striped');
				table.append('<tr><th>Category name</th><th>Count</th></tr>');
				$.each(aggregateResult.hashCategories, function(categoryName,
						count) {
					table.append('<tr><td>' + categoryName + '</td><td>'
							+ count + '</td></tr>');
				});
				$('#aggregate-table-container').empty().append(table);
			},
			error : function(xhr, tst, err) {
				$.log('XHR ERROR ' + XMLHttpRequest.status);
			}
		});
	};

	molgenis.filterDialog = function() {
		$.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/filterdialog',
			data : JSON.stringify({'entityUri' : $('#dataset-select').val()}),
			contentType : 'application/json',
			success : function(result) {
				$(function() {
					var modal = $('#filter-dialog-modal').html(result);
					$('#filter-dialog-modal').show();
				});
			}
		});
	}

	molgenis.download = function() {
		var jsonRequest = JSON.stringify(molgenis.createSearchRequest(false));
		parent.showSpinner();
		$.download(molgenis.getContextUrl() + '/download', {
			searchRequest : jsonRequest
		});
		parent.hideSpinner();
	};

	//--BEGIN genome browser--
	molgenis.updateGenomeBrowser = function(dataSet) {
		if (dataSet.identifier in genomeBrowserDataSets) {
			document.getElementById('genomebrowser').style.display = 'block';
			document.getElementById('genomebrowser').style.visibility = 'visible';
			dalliance.reset();
			var dallianceTrack = [{
				name : dataSet.name,
				uri : '/das/molgenis/dataset_' + dataSet.identifier + '/',
				desc : "Selected dataset",
				stylesheet_uri : '/css/selected_dataset-track.xml'
			}];
			dalliance.addTier(dallianceTrack[0]);
			$.each(genomeBrowserDataSets, function(dataSetIdentifier,
					dataSetName) {
				if (dataSetIdentifier != molgenis
						.getSelectedDataSetIdentifier()) {
					var dallianceTrack = [{
						name : dataSetName,
						uri : '/das/molgenis/dataset_' + dataSetIdentifier
								+ '/',
						desc : "unselected dataset",
						stylesheet_uri : '/css/not_selected_dataset-track.xml'
					}];
					dalliance.addTier(dallianceTrack[0]);
				}
			});
		} else {
			document.getElementById('genomebrowser').style.display = 'none';
		}
	}

	molgenis.setDallianceFilter = function() {
		var start = molgenis.getFeatureByIdentifier('start_nucleotide');
		var chromosome = molgenis.getFeatureByIdentifier('chromosome');
		molgenis.updateFeatureFilter(start.href, {
			name : start.name,
			identifier : start.identifier,
			type : start.dataType,
			range : true,
			values : [Math.floor(dalliance.viewStart).toString(),
					Math.floor(dalliance.viewEnd).toString()]
		});
		molgenis.updateFeatureFilter(chromosome.href, {
			name : chromosome.name,
			identifier : chromosome.identifier,
			type : chromosome.dataType,
			values : [dalliance.chr]
		});
	}
	molgenis.getFeatureByIdentifier = function(identifier) {
		var feature;
		var searchHit = restApi.get('/api/v1/observablefeature', null, {
			q : [{
				field : 'identifier',
				operator : 'EQUALS',
				value : identifier
			}],
			num : 500
		});
		if (searchHit.items.length > 0) {
			feature = searchHit.items[0];
		}
		return feature;
	}
	//--END genome browser--

	// on document ready
	$(function() {
		// use chosen plugin for data set select
		$('#dataset-select').chosen();
		$('#dataset-select').change(
				function() {
					var checkDataViewer = $('#dataexplorer-grid-data').is(':visible');
					if (checkDataViewer)
						molgenis.onDataSetSelectionChange($(this).val());
					else
						molgenis.initializeAggregate($(this).val());
				});
		
		resultsTable = new molgenis.ResultsTable();

		$('a[data-toggle="tab"][href="#dataset-aggregate-container"]')
				.on(
						'shown',
						function(e) {
							molgenis.initializeAggregate($('#dataset-select')
									.val());
							molgenis
									.createFeatureSelectionTree($('#dataset-select').val());
						});

		$("#observationset-search").focus();
		
		$("#observationset-search").change(function(e) {
			molgenis.searchObservationSets($(this).val());
		});

		$('#wizard-button').click(function() {
			molgenis.filterDialog();
		});

		$('.feature-filter-dialog').dialog({
			modal : true,
			width : 500,
			autoOpen : false
		});

		$('#download-button').click(function() {
			molgenis.download();
		});

		$('#genomebrowser-filter-button').click(function() {
			molgenis.setDallianceFilter();
		});

		// fire event handler
		$('#dataset-select').change();
	});
}($, window.top.molgenis = window.top.molgenis || {}));