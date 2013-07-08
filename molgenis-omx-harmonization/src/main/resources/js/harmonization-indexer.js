(function($, w) {
	
	"use strict";
	var ns = w.molgenis = w.molgenis || {};
	var restApi = new ns.RestClient();
	var searchApi = new ns.SearchClient();
	var selectedDataSet = null;
	var offSet = 1;
	var currentPage = 1;
	var pager = 10;
	var totalPage = 0;
	
	ns.changeDataSet = function(selectedDataSet){
		resetVariables();
		ns.updateSelectedDataset(selectedDataSet);
		ns.createMatrixForDataItems();
		function resetVariables(){
			offSet = 1;
			currentPage = 1;
		}
	};
	
	ns.createMatrixForDataItems = function() {
		searchApi.search(ns.createSearchRequest(), function(searchResponse) {
			createTableHeader();
			var searchHits = searchResponse.searchHits;
			$.each(searchHits, function(){
				var feature = $(this)[0]["columnValueMap"];
				var description = feature.description;
				var isPopOver = description.length < 50;
				var popover = $('<span />');
				popover.html(isPopOver ? description : description.substring(0, 50) + '...');
				if(!isPopOver){
					popover.addClass('show-popover');
					popover.popover({
						content : description,
						trigger : 'hover',
						placement : 'bottom'
					});
				}
				var clickFeature = $('<span class="text-info show-popover">' + feature.name + '</span>');
				clickFeature.data('feature', feature);
				clickFeature.click(function(){
					var featureId = $(this).data('feature').id;
					restApi.getAsync('/api/v1/observablefeature/' + featureId, ["unit", "definition"], null, function(feature){
						createAnnotationModal(feature);
					});
				});
				
				var row = $('<tr />');
				$('<td />').append(clickFeature).appendTo(row);
				$('<td />').append(popover).appendTo(row);
				var annotation = $('<td />');
				var featureEntity = restApi.get('/api/v1/observablefeature/' + feature.id, ["unit", "definition"]);
				if(featureEntity.definition.items !== 0){
					var annotationText = '';
					$.each(featureEntity.definition.items, function(index, ontologyTerm){
						annotationText += ontologyTerm.name + ',';
					});
					annotationText.substring(0, annotationText.length - 2);
					annotation.append(annotationText);
				}
				
				annotation.appendTo(row);
				$(row).appendTo($('#dataitem-table'));
			});
			totalPage = Math.ceil(searchResponse.totalHitCount / pager) - 1;
			ns.updateMatrixPagination();
		});
		
		function createAnnotationModal(feature) {
			
			var modal = $('<div />');
			if($('#annotation-modal').length != 0){
				modal = $('#annotation-modal');
				modal.empty();
			}else{
				$('body').append(modal);
			}
			modal.addClass('modal hide');
			modal.attr('id', 'annotation-modal');
			modal.attr('data-backdrop', false);
			
			var header = $('<div />');
			header.addClass('modal-header');
			header.append('<button type="button" name="annotation-btn-close" class="close" data-dismiss="#annotation-modal" data-backdrop="true" aria-hidden="true">&times;</button>');
			header.append("<h3>Annotate data item</h3>");
			
			var body = $('<div />');
			body.addClass('modal-body');
			body.append(featureTable(feature));
			body.append(createSearchDiv(feature));
			
			var footer = $('<div />');
			footer.addClass('modal-footer');
			footer.append('<button name="annotation-btn-close" class="btn" data-dismiss="#annotation-modal" aria-hidden="true">Close</button>');
			
			modal.append(header);
			modal.append(body);
			modal.append(footer);
			modal.modal('show');
			
			$('button[name="annotation-btn-close"]').click(function(){
				$('#annotation-modal').modal('hide');
			});
		}
		
		function createSearchDiv(feature){
			var searchDiv = $('<div class="row-fluid"></div>');
			var searchGroup = $('<div class="input-append span4"></div>');
			var searchField = $('<input type="text" data-provide="typeahead" />');
			var addTermButton = $('<button class="btn" type="button" id="add-ontologyterm-button">Add</button>');
			searchField.appendTo(searchGroup);
			addTermButton.appendTo(searchGroup);
			searchField.typeahead({
				source: function(query, process) {
					ns.searchOntologyTerms(query, process);
				},
				minLength : 3,
				items : 10
			});
			addTermButton.click($.proxy(checkOntologyTerm, {'searchField' : searchField, 'feature' : feature}));
			return searchDiv.append(searchGroup);
		}
		
		function checkOntologyTerm(){
			var dataMap = $(document).data('dataMap');
			var ontologyTerm = this.searchField.val();
			var toCreate = true;
			if(dataMap && ontologyTerm !== ''){
				var uri = dataMap[ontologyTerm].ontologyTermIRI;
				var q = {
						q : [ {
							field : 'termAccession',
							operator : 'EQUALS',
							value : uri
						} ],
				};
				restApi.getAsync('/api/v1/ontologyterm/', null, q, $.proxy(function(result){
					var ontologyTermId = null;
					if(result.items.length !== 0) {
						toCreate = false;
						var href = result.items[0].href;
						ontologyTermId = href.substring(href.lastIndexOf('/') + 1);
					}
					if(toCreate) ontologyTermId = createOntologyTerm(dataMap[ontologyTerm]);
					if(ontologyTermId != null) updateAnnotation(restApi.get(this.feature.href), ontologyTermId, true);
					$('#annotation-modal').modal('hide');
					restApi.getAsync(this.feature.href, ["unit", "definition"], null, function(updatedFeature){
						createAnnotationModal(updatedFeature);
						ns.createMatrixForDataItems();
					});
				},{'feature' : this.feature}));
			}
		}

		function updateAnnotation(feature, ontologyTermId, add){
			var data = {};
			$.map(feature, function(value, key){
				if(key !== 'href'){
					if(key === 'unit')
						data[key] = value.href.substring(value.href.lastIndexOf('/') + 1);
					else if(key === 'definition'){
						data[key] = [];
						$.each(value.items, function(index, element){
							data[key].push(element.href.substring(element.href.lastIndexOf('/') + 1));
						});
					}else
						data[key] = value;
				}	
			});
			if($.inArray(ontologyTermId, data.definition) === -1 && add) data.definition.push(ontologyTermId);
			if($.inArray(ontologyTermId, data.definition) !== -1 && !add) {
				var index = data.definition.indexOf(ontologyTermId);
				data.definition.splice(index, 1);
			}
			updateFeature(feature, data);
		}
		
		function updateFeature(feature, data){
			$.ajax({
				type : 'PUT',
				dataType : 'json',
				url : feature.href,
				cache: true,
				data : JSON.stringify(data),
				contentType : 'application/json',
				async : false,
				success : function(data, textStatus, request) {
					console.log(data);
				},
				error : function(request, textStatus, error){
					console.log(error);
				} 
			});
		}
		
		function createOntologyTerm(data){
			var ontologyTermId = null;
			var query = {};
			query.name = data.ontologyTerm;
			query.identifier = data.ontologyTermIRI;
			query.termAccession = data.ontologyTermIRI;
			query.description = data.ontologyTermLabel;
			$.ajax({
				type : 'POST',
				dataType : 'json',
				url : '/api/v1/ontologyterm/',
				cache: true,
				data : JSON.stringify(query),
				contentType : 'application/json',
				async : false,
				success : function(data, textStatus, request) {
					var href = request.getResponseHeader('Location');
					ontologyTermId = href.substring(href.lastIndexOf('/') + 1);
				},
				error : function(request, textStatus, error){
					console.log(error);
				} 
			});
			return ontologyTermId;
		}
		
		function featureTable(feature){
			var table = $('<table class="table table-bordered"></table>'); 
			
			if(feature.description === undefined) feature.description = '';
			if(feature.description.indexOf('{') !== 0){
				feature.description = '{"en":"' + (feature.description === null ? '' : feature.description) +'"}';
			}
			var description = eval('(' + feature.description + ')');
			table.append('<tr><th>ID : </th><td>' + feature.href.substring(feature.href.lastIndexOf('/') + 1) + '</td></tr>');
			table.append('<tr><th>Name : </th><td>' + feature.name + '</td></tr>');
			table.append('<tr><th>Description : </th><td>' + (description === undefined ? '' : description.en) + '</td></tr>');
			if(feature.definition.items.length !== 0){
				var ontologyTermAnnotations = $('<ul />');
				$.each(feature.definition.items, function(index, element){
					var uri = element.termAccession;
					var linkOut = $('<a href="' + uri + '" target="_blank">' + element.name + '</a>');
					var removeIcon = $('<i class="icon-remove"></i>');
					$('<li />').append(linkOut).append(removeIcon).appendTo(ontologyTermAnnotations);
					removeIcon.click($.proxy(function(){
						var ontologyTermId = this.ontologyTermHref.substring(this.ontologyTermHref.lastIndexOf('/') + 1)
						updateAnnotation(this.feature, ontologyTermId, false);
						$('#annotation-modal').modal('hide');
						restApi.getAsync(this.feature.href, ["unit", "definition"], null, function(updatedFeature){
							createAnnotationModal(updatedFeature);
							ns.createMatrixForDataItems();
						});
					}, {'feature' : feature, 'ontologyTermHref' : element.href}));
				});
				var annotationRow = $('<tr />');
				annotationRow.append('<th>Annotation : </th>');
				annotationRow.append($('<td />').append(ontologyTermAnnotations));
				table.append(annotationRow);
			}else{
				table.append('<tr><th>Annotation : </th><td>Not available</td></tr>');
			}
			return table;
		}
		
		function createTableHeader(){
			$('#dataitem-table').empty();
			$('#dataitem-table').append('<thead><tr><th>Name</th><th>Description</th><th>Annotation</th></tr></thead>');
		}
	};
	
	ns.updateMatrixPagination = function() {
		if(totalPage !== 0){
			$('.pagination ul').empty();
			$('.pagination ul').append('<li><a href="#">Prev</a></li>');
			var displayedPage = (totalPage < 10 ? totalPage : 9) + offSet; 
			for(var i = offSet; i <= displayedPage ; i++){
				var element = $('<li />');
				if(i == currentPage)
					element.addClass('active');
				element.append('<a href="/">' + i + '</a>');
				$('.pagination ul').append(element);
			}
			var lastPage = totalPage + 1 > 10 ? totalPage + 1 : 10;
			if(totalPage - offSet > 9){
				$('.pagination ul').append('<li class="active"><a href="#">...</a></li>');
				$('.pagination ul').append('<li><a href="#">' + lastPage + ' </a></li>');
			}
			$('.pagination ul').append('<li><a href="#">Next</a></li>');
			$('.pagination ul li').each(function(){
				$(this).click(function(){
					var pageNumber = $(this).find('a').html();
					if(pageNumber === "Prev"){
						if(currentPage > offSet) currentPage--;
						else if(offSet > 1) {
							offSet--;
							currentPage--;	
						}
					}else if(pageNumber === "Next"){
						if(currentPage <= totalPage) {
							currentPage++;
							if(currentPage >= offSet + 9) offSet++;
						}
					}else if(pageNumber !== "..."){
						currentPage = parseInt(pageNumber);
						if(currentPage > offSet + 9){
							offSet = currentPage - 9;
						} 
					}
					ns.createMatrixForDataItems();
					return false;
				});
			});
		}
	};
	
	ns.createSearchRequest = function() {
		var queryRules = [];
		//todo: how to unlimit the search result
		queryRules.push({
			operator : 'LIMIT',
			value : pager
		});
		queryRules.push({
			operator : 'OFFSET',
			value : (currentPage - 1) * pager
		});
		queryRules.push({
			operator : 'SEARCH',
			value : 'observablefeature'
		});
		var searchRequest = {
			documentType : 'protocolTree-' + getSelectedDataSet(),
			queryRules : queryRules
		};
		return searchRequest;
	};
	
	ns.updateSelectedDataset = function(dataSet) {
		selectedDataSet = dataSet;
	};
	
	ns.annotateDataItems = function() {
		$('input[name="__action"]').val("annotateDataItems");
		$('#harmonizationIndexer-form').submit();
	};
	
	ns.searchOntologyTerms = function (query, response){
		
		var queryRules = [{
			field : 'ontologyTermSynonym',
			operator : 'EQUALS',
			value : query,
		},{
			operator : 'LIMIT',
			value : 20
		}];
		
		var searchRequest = {
			documentType : null,
			queryRules : queryRules
		};
		
		searchApi.search(searchRequest, function(searchReponse){
			var result = [];
			var dataMap = {};
			$.each(searchReponse.searchHits, function(index, hit){
				var value = hit.columnValueMap.ontologyTerm;
				if($.inArray(value, result) === -1){
					result.push(hit.columnValueMap.ontologyTerm);
					dataMap[hit.columnValueMap.ontologyTerm] = hit.columnValueMap;
				}
			});
			$(document).data('dataMap', dataMap);
			response(result);
		});
	};
	
	ns.selectCatalogue = function(){
		var dataSets = restApi.get('/api/v1/dataset/');
		var catalogueIds = [];
		if(dataSets.items.length > 0){
			$.each(dataSets.items, function(index, item){
				var href = item.href;
				if(getSelectedDataSet() !== href.substring(href.lastIndexOf('/') + 1)){
					catalogueIds.push(item);
				}
			});
		}
		createSelectModal(catalogueIds);
		
		function createSelectModal (catalogueIds){
			
			var container = $('<div />');
			container.addClass('modal hide fade in');
			container.attr({
				'tabindex' : -1,
				'role' : 'dialog',
				'aria-hidden' : true
			});
			
			var header = $('<div class="modal-header"></div>');
			header.append('<strong>Select catalogue(s) to match</strong></br>');
			
			var body = $('<div class="modal-body"></div>');
			var select = $('<select name="listOfCohortStudies" style="width:185px;"></select>');
			var addNewIcon = $('<i class="icon-plus" style="cursor:pointer;margin-left:2px;" title="add studies"></i>');
			var table = $('<table class="table table-striped table-bordered"></table>');
			$('<tr />').append('<th style="width:60%;">Selected catalogue</th>').append('<th style="width:40%;">Remove</th>').appendTo(table);
			body.append(select).append(addNewIcon).append(table).append('<input name="selectedStudiesToMatch" type="hidden"/>');
			
			$.each(catalogueIds, function(index, item){
				var href = item.href;
				select.append('<option value="' + href.substring(href.lastIndexOf('/') + 1) + '">' + item.name + '</option>');
			});
			
			addNewIcon.click($.proxy(function(){
				var removeIcon = $('<i class=\"icon-remove\" style=\"cursor:pointer;\"></i>');
				var newCell = $('<td />').append(removeIcon);
				var row = $('<tr />').append('<td>' + this.select.find('option:selected').text() + '</td>').append(newCell).appendTo(this.table);
				var selectedOption = this.select.find('option:selected').remove();
				row.data('dataSet', selectedOption.val());
				
				removeIcon.click($.proxy(function(){
					this.select.append(this.selectedOption);
					this.removeIcon.parents('tr:eq(0)').remove();
				},{'select' : this.select, 'table' : this.table, 'removeIcon' : removeIcon, 'selectedOption' : selectedOption}));
				
			},{'select' : select, 'table' : table}));
			
			
			var footer = $('<div class="modal-footer"></div>');
			var matchButton = $('<button id="start-match" class="btn btn-primary">Start match</button>');
			var cancel = $('<button id="cancel-match" class="btn btn-primary" data-dismiss="modal">Cancel</button>');
			footer.append(matchButton).append(cancel);
			matchButton.click($.proxy(function(){
				var selectedStudies = {};
				this.table.find('tr:gt(0)').each(function(){
					selectedStudies[$(this).data('dataSet')] =$(this).data('dataSet');
				});
				$('input[name=\"selectedStudiesToMatch\"]').val(JSON.stringify(selectedStudies));
				$('input[name="__action"]').val("ontologyMatch");
				$('#harmonizationIndexer-form').submit();
			},{'table' : table}));
			cancel.click(function(){
				container.remove();
			});
			
			container.append(header).append(body).append(footer);
			$('#harmonizationIndexer-form').append(container);
			container.modal('backdrop', true);
			container.show();
		}
	};
	
	function getSelectedDataSet(){
		return selectedDataSet;
	}
	
	$(function() {
		$('#index-button').click(function(){
			if($('#uploadedOntology').val() !== ''){
				$('input[name="__action"]').val("indexOntology");
				$('#harmonizationIndexer-form').submit();
			}else{
				alert('Please upload a file in OWL or OBO format!');
			}
		});
		$('#refresh-button').click(function(){
			$('#harmonizationIndexer-form').submit();
		});
		$('#match-catalogue').click(function(){
			ns.selectCatalogue();
			return false;
		});
		$('#annotate-dataitems').click(function(){
			ns.annotateDataItems();
		});
	});
}($, window.top));