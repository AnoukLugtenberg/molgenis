(function($, molgenis) {
	"use strict";

	var sortRule = null;

	molgenis.setEntityExplorerUrl = function(entityExplorerUrl) {
		molgenis.entityExplorerUrl = entityExplorerUrl;
	};
	
	molgenis.ResultsTable = function ResultsTable() {
	};

	molgenis.ResultsTable.prototype.getMaxRows = function() {
		return 20;
	};

	molgenis.ResultsTable.prototype.getSortRule = function() {
		return sortRule;
	};

	molgenis.ResultsTable.prototype.resetSortRule = function() {
		sortRule = null;
	};

	molgenis.ResultsTable.prototype.build = function(searchResponse, selectedFeatures, restApi) {
		var nrRows = searchResponse.total;

		var items = [];
		items.push('<thead>');
		$.each(selectedFeatures, function(i, val) {
			var feature = restApi.get(this);
			if (sortRule && sortRule.orders[0].property == feature.name) {
				if (sortRule.orders[0].direction == 'ASC') {
					items.push('<th>' + feature.label + '<span data-value="' + feature.name
							+ '" class="ui-icon ui-icon-triangle-1-s down"></span></th>');
				} else {
					items.push('<th>' + feature.label + '<span data-value="' + feature.name
							+ '" class="ui-icon ui-icon-triangle-1-n up"></span></th>');
				}
			} else {
				items.push('<th>' + feature.label + '<span data-value="' + feature.name
						+ '" class="ui-icon ui-icon-triangle-2-n-s updown"></span></th>');
			}
		});
		items.push('</thead>');

		items.push('<tbody>');

		if (nrRows == 0) {
			items.push('<tr><td class="nothing-found" colspan="' + selectedFeatures.length + '">Nothing found</td></tr>');
		}

		for ( var i = 0; i < searchResponse.items.length; ++i) {
			items.push('<tr>');
			var columnValueMap = searchResponse.items[i];

			$.each(selectedFeatures, function(i, val) {
				var feature = restApi.get(this);
				var key = (feature.name).charAt(0).toLowerCase() + feature.name.slice(1);
				var value = columnValueMap[key];
				var cellValue = "";
				if ((value != null) && (value != undefined)) {
					if (feature.fieldType === "XREF" && (typeof molgenis.entityExplorerUrl !== "undefined")){
						var attributeName = restApi.get(value.href).identifier;
						cellValue = '<a href="'+ molgenis.entityExplorerUrl +'?entity=Characteristic&identifier=' + attributeName + '">' 
							+ formatTableCellValue(attributeName, feature.fieldType) + '</a>';
					}	
					else if (feature.fieldType === "MREF" && (typeof molgenis.entityExplorerUrl !== "undefined")){
						var itemsMref = restApi.get(value.href).items;
						for(var i=0; i < itemsMref.length; i++){
						    if(i > 0) cellValue +=  ',';
						    cellValue += '<a href="'+ molgenis.entityExplorerUrl +'?entity=Characteristic&identifier=' + itemsMref[i].identifier + '">' 
						    	+ formatTableCellValue(itemsMref[i].identifier, feature.fieldType) + '</a>';
						}
					}
					else{
						cellValue = formatTableCellValue(value, feature.fieldType);
					}
					items.push('<td class="multi-os-datacell">' + cellValue + '</td>');
				} else {
					items.push('<td></td>');
				}
			});

			items.push('</tr>');
		}
		items.push('</tbody>');
		$('#data-table').html(items.join(''));
		$('.show-popover').popover({trigger:'hover', placement: 'bottom'});
		
		// Sort click
		$('#data-table thead th .ui-icon').click(function() {
			if (nrRows == 0) {
				return;
			}

			var featureIdentifier = $(this).data('value');
			console.log("select sort column: " + featureIdentifier);
			if (sortRule && sortRule.orders[0].direction == 'ASC') {
				sortRule = {
						orders: [{
							property: featureIdentifier,
							direction: 'DESC'
						}]
				};
			} else {
				sortRule = {
						orders: [{
							property: featureIdentifier,
							direction: 'ASC'
						}]
				};
			}

			molgenis.updateObservationSetsTable();
			return false;
		});
	};

}($, window.top.molgenis = window.top.molgenis || {}));