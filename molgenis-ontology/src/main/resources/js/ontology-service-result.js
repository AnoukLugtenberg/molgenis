(function($, molgenis) {
	"use strict";
	
	var restApi = new molgenis.RestClient();
	var ontologyServiceRequest = null;
	var result_container = null;
	var reserved_identifier_field = 'Identifier';
	var NO_MATCH_INFO = 'N/A';
	var itermsPerPage = 5;
	
	molgenis.OntologyService = function OntologySerivce(container, request){
		result_container = container;
		ontologyServiceRequest = request;
	};
	
	molgenis.OntologyService.prototype.updatePageFunction = function(page){
		ontologyServiceRequest['entityPager'] = {
			'start' : page.start,
			'num' : itermsPerPage,
			'total' : page.end,
		};
		$.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/match/retrieve',
			async : false,
			data : JSON.stringify(ontologyServiceRequest),
			contentType : 'application/json',
			success : function(data) {
				result_container.empty();
				if(data.items.length > 0){
					var pagerDiv = $('<div />').addClass('row').appendTo(result_container);
					var searchItems = [];
					searchItems.push('<div class="col-md-3">');
					searchItems.push('<div class="input-group"><span class="input-group-addon">Filter</span>');
					searchItems.push('<input type="text" class="form-control" value="' + (ontologyServiceRequest.filterQuery ? ontologyServiceRequest.filterQuery : '') + '" />');
					searchItems.push('<span class="input-group-btn"><button class="btn btn-default"><span class="glyphicon glyphicon-search"></span></button></span>')
					searchItems.push('</div></div>')
					
					var matchResultHeaderDiv = $('<div />').addClass('row').css({'margin-bottom':'10px'}).appendTo(result_container);
					matchResultHeaderDiv.append(searchItems.join(''));
					matchResultHeaderDiv.append('<div class="col-md-6"><center><strong><p style="font-size:20px;">' + (ontologyServiceRequest.matched ? 'Matched result' : 'Unmatched result') + '</p></strong></center></div>');
					
					var tableItems = [];
					tableItems.push('<div class="col-md-12"><table class="table">');
					tableItems.push('<tr><th style="width:38%;">Input term</th><th style="width:38%;">Best candidate</th><th style="width:10%;">Score</th><th style="width:10%;">Manual Match</th>' + (ontologyServiceRequest.matched ? '<th>Remove</th>' : '') + '</tr>');
					tableItems.push('</table></div>');
					$('<div />').addClass('row').append(tableItems.join('')).appendTo(result_container);
					var table = $(result_container).find('table:eq(0)')
					
					$.each(data.items, function(index, entity){
						table.append(createRowForMatchedTerm(entity, ontologyServiceRequest.matched, page));
					});
					
					var searchButton = matchResultHeaderDiv.find('button:eq(0)');
					var searchBox = matchResultHeaderDiv.find('input:eq(0)');
					$(searchButton).click(function(){
						if(ontologyServiceRequest.filterQuery !== $(searchBox).val()){
							ontologyServiceRequest.filterQuery = $(searchBox).val();
							molgenis.OntologyService.prototype.updatePageFunction(page);
						}
						return false;
					});
					
					$(searchBox).keyup(function(e){
						//stroke key enter or backspace 
						if(e.keyCode === 13 || $(this).val() === ''){
							$(searchButton).click();
						}
					});
					
					$(pagerDiv).pager({
						'page' : Math.floor(data.start / data.num) + (data.start % data.num == 0 ? 0 : 1) + 1,
						'nrItems' : data.total,
						'nrItemsPerPage' : data.num,
						'onPageChange' : molgenis.OntologyService.prototype.updatePageFunction
					});
				}else{
					var messageItems = [];
					messageItems.push('<div class="col-md-offset-3 col-md-6"><p>There are no results!</p>');
					if(ontologyServiceRequest.filterQuery){
						messageItems.push('<strong>Clear the query </strong>: ' + ontologyServiceRequest.filterQuery + '&nbsp;&nbsp;');
						messageItems.push('<span class="glyphicon glyphicon-remove"></span>');
					}
					messageItems.push('<br><br></div>');
					result_container.append(messageItems.join(''));
					$(result_container).find('span.glyphicon-remove:eq(0)').click(function(){
						ontologyServiceRequest.filterQuery = '';
						molgenis.OntologyService.prototype.updatePageFunction(page);
					});
				}
			}
		});
	};
	
	molgenis.OntologyService.prototype.deleteMatchingTask = function(entityName, callback){
		$.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/delete',
			async : false,
			data : JSON.stringify(entityName),
			contentType : 'application/json',
			success : function() {
				if(callback) callback();
			}
		});
	};
	
	function createRowForMatchedTerm(responseData, matched, page){
		var row = $('<tr />');
		row.append(gatherInputInfoHelper(responseData.inputTerm));
		row.append(gatherOntologyInfoHelper(responseData.inputTerm, responseData.ontologyTerm));
		$('<td />').append(responseData.matchedTerm.Score ? responseData.matchedTerm.Score.toFixed(2) + '%' : NO_MATCH_INFO).appendTo(row);
		if(matched){
			$('<td />').append('<span class="glyphicon ' + (responseData.matchedTerm.Validated ? 'glyphicon-ok' : 'glyphicon-remove') + '"></span>').appendTo(row);
			$('<td />').append(responseData.matchedTerm.Validated ? '<button type="button" class="btn btn-default"><span class="glyphicon glyphicon-trash"</span></button>':'').appendTo(row);
			row.find('button:eq(0)').click(function(){
				matchEntity(responseData.inputTerm.Identifier, ontologyServiceRequest.entityName, function(data){
					var updatedMappedEntity = {};
					$.map(responseData.matchedTerm, function(val, key){
						if(key !== 'Identifier') updatedMappedEntity[key] = val;
						if(key === 'Validated') updatedMappedEntity[key] = false;
					});
					if(data.ontologyTerms && data.ontologyTerms.length > 0){
						var ontologyTerm = data.ontologyTerms[0];
						updatedMappedEntity['Score'] = ontologyTerm.Score;
						updatedMappedEntity['Match_term'] = ontologyTerm.ontologyTermIRI;
					}else{
						updatedMappedEntity['Score'] = 0;
						updatedMappedEntity['Match_term'] = null;
					}
					restApi.update('/api/v1/MatchingTaskContent/' + responseData.matchedTerm.Identifier, updatedMappedEntity);
					molgenis.OntologyService.prototype.updatePageFunction(page);
				});
			});
		}else{
			var button = $('<button class="btn btn-default" type="button">Match</button>').click(function(){
				matchEntity(responseData.inputTerm.Identifier, ontologyServiceRequest.entityName, function(data){
					createTableForCandidateMappings(responseData.inputTerm, data, row, page);
				})
			});
			$('<td />').append(button).appendTo(row);
		}
		return row;
	}
	
	function createTableForCandidateMappings(inputEntity, data, row, page){
		
		var container = $('<div class="row"></div>').css({'margin-bottom':'20px'});
		//Hide existing table
		row.parents('table:eq(0)').hide();
		//Add table containing candidate matches to the view
		row.parents('div:eq(0)').append(container);
		
		//Add a backButton for users to go back to previous summary table
		var backButton = $('<button type="button" class="btn btn-warning">Cancel</button>').css({'margin-bottom':'10px','float':'right'}).click(function(){
			container.remove();
			row.parents('table:eq(0)').show();
		});
		//Add a unknownButton for users to choose 'Unknown' for the input term
		var unknownButton = $('<button type="button" class="btn btn-danger">No match</button>').
			css({'margin-bottom':'10px','margin-right':'10px','float':'right'}).click(function(){
			getMappingEntity(inputEntity.Identifier, ontologyServiceRequest.entityName, function(data){
				if(data.items.length > 0){
					var mappedEntity = data.items[0];
					var href = '/api/v1/MatchingTaskContent/' + mappedEntity.Identifier;
					var updatedMappedEntity = {};
					$.map(mappedEntity, function(val, key){
						if(key !== 'Identifier') updatedMappedEntity[key] = val;
						if(key === 'Validated') updatedMappedEntity[key] = true;
						if(key === 'Score') updatedMappedEntity[key] = 0;
						if(key === 'Match_term') updatedMappedEntity[key] = null;
					});
					restApi.update(href, updatedMappedEntity);
					molgenis.OntologyService.prototype.updatePageFunction(page);
				}
			});
		});
		
		var hoverover = $('<div>Adjusted score ?</div>').css({'cursor':'pointer'}).popover({
			'title' : 'Explanation',
			'content' : '<p style="color:black;font-weight:normal;">Adjusted scores are derived from the original scores (<strong>lexical similarity</strong>) combined with the weight of the words (<strong>inverse document frequency</strong>)</p>',
			'placement' : 'top',
			'trigger' : 'hover',
			'html' : true
		});
		
		var table = $('<table class="table"></table>');
		var header = $('<tr />').appendTo(table);
		$('<th />').append('Input term').css({'width':'30%'}).appendTo(header);
		$('<th />').append('Candidate mapping').css({'width':'40%'}).appendTo(header);
		$('<th />').append('Score').css({'width':'12%'}).appendTo(header);
		$('<th />').append(hoverover).css({'width':'12%'}).appendTo(header);
		$('<th />').append('Select').appendTo(header);
		
		var hintInformation;
		if(data.ontologyTerms && data.ontologyTerms.length > 0){
			hintInformation = $('<center><p style="font-size:15px;">The candidate ontology terms are sorted based on similarity score, please select one of them by clicking <span class="glyphicon glyphicon-ok"></span> button</p></center>');
			$.each(data.ontologyTerms, function(index, ontologyTerm){
				if(index >= 20) return;
				var row = $('<tr />').appendTo(table);
				row.append(index == 0 ? gatherInputInfoHelper(inputEntity) : '<td></td>');
				row.append(gatherOntologyInfoHelper(inputEntity, ontologyTerm));
				row.append('<td>' + ontologyTerm.Score.toFixed(2) + '%</td>');
				row.append('<td>' + ontologyTerm.Combined_Score.toFixed(2) + '%</td>');
				row.append('<td><button type="button" class="btn btn-default"><span class="glyphicon glyphicon-ok"></span></button></td>');
				row.data('ontologyTerm', ontologyTerm);
				row.find('button:eq(0)').click(function(){
					getMappingEntity(inputEntity.Identifier, ontologyServiceRequest.entityName, function(data){
						if(data.items.length > 0){
							var mappedEntity = data.items[0];
							var href = '/api/v1/MatchingTaskContent/' + mappedEntity.Identifier;
							var updatedMappedEntity = {};
							$.map(mappedEntity, function(val, key){
								if(key === 'Validated') updatedMappedEntity[key] = true;
								else if(key === 'Match_term') updatedMappedEntity['Match_term'] = row.data('ontologyTerm').ontologyTermIRI;
								else if(key === 'Score') updatedMappedEntity['Score'] = row.data('ontologyTerm').Score;
								else if(key !== 'Identifier') updatedMappedEntity[key] = val;
							});
							restApi.update(href, updatedMappedEntity);
							molgenis.OntologyService.prototype.updatePageFunction(page);
						}
					});
				});
			});
		}else{
			hintInformation = $('<center><p style="font-size:15px;">There are no candidate mappings for this input term!</p></center>');
			$('<tr />').append(gatherInputInfoHelper(inputEntity)).append('<td>' + NO_MATCH_INFO + '</td><td>' + NO_MATCH_INFO + '</td><td>' + NO_MATCH_INFO + '</td><td>' + NO_MATCH_INFO + '</td>').appendTo(table);
		}
		$('<div class="col-md-12"></div>').append(hintInformation).append(backButton).append(unknownButton).append(table).appendTo(container);
	}
	
	function getMappingEntity(inputTermIdentifier, entityName, callback){
		var mappedEntity = restApi.getAsync('/api/v1/MatchingTaskContent/', {
			'q' : [{
				'field' : 'Input_term',
				'operator' : 'EQUALS',
				'value' : inputTermIdentifier
			},{'operator' : 'AND'},{
				'field' : 'Ref_entity',
				'operator' : 'EQUALS',
				'value' : entityName
			}]
		}, function(data){
			if(callback) callback(data);
		});
	}
	
	function matchEntity(inputTermIdentifier, entityName, callback){
		$.ajax({
			type : 'POST',
			url : molgenis.getContextUrl() + '/match/entity',
			async : false,
			data : JSON.stringify({'Identifier' : inputTermIdentifier, 'entityName' : entityName}),
			contentType : 'application/json',
			success : function(data) {
				if(callback) callback(data);
			}
		});
	}
	
	function gatherInputInfoHelper(inputTerm){
		var inputTermTd = $('<td />');
		if(inputTerm){
			$.map(inputTerm ? inputTerm : {}, function(val, key){
				if(key !== reserved_identifier_field) inputTermTd.append('<div>' + key + ' : ' + val + '</div>');
			});
		}
		return inputTermTd;
	}
	
	function gatherOntologyInfoHelper(inputEntity, ontologyTerm){
		var ontologyTermTd = $('<td />');
		if(inputEntity && ontologyTerm){
			var synonymDiv = $('<div>Synonym : </div>');
			var synonyms = getOntologyTermSynonyms(ontologyTerm);
			if(synonyms.length == 0){
				synonymDiv.append(NO_MATCH_INFO);
			}else if(synonyms.length == 1){
				synonymDiv.append(synonyms.join());		
			}else{
				synonymDiv.addClass('show-popover').append('<strong>' + synonyms.length + ' synonyms, see more details</strong>').popover({
					'content' : synonyms.join('<br><br>'),
					'placement' : 'auto',
					'trigger': 'hover',
					'html' : true
				});
			}
			ontologyTermTd.append('<div>Name : <a href="' + ontologyTerm.ontologyTermIRI + '" target="_blank">' + ontologyTerm.ontologyTermName + '</a></div>').append(synonymDiv);
			var annotationMap = {};
			$.each(ontologyTerm.ontologyTermDynamicAnnotation, function(i, annotation){
				if(!annotationMap[annotation.name]){
					annotationMap[annotation.name] = [];
				}
				annotationMap[annotation.name].push(annotation.value);
			});
			$.each(Object.keys(inputEntity), function(index, key){
				if(key.toLowerCase() !== 'name' && key.toLowerCase().search('synonym') === -1 && key.toLowerCase() !== reserved_identifier_field.toLowerCase()){
					ontologyTermTd.append('<div>' + key + ' : ' + (annotationMap[key] ? annotationMap[key].join() : 'N/A')  + '</div>');
				}
			});
		}else{
			ontologyTermTd.append(NO_MATCH_INFO);
		}
		return ontologyTermTd;
	}
	
	function getOntologyTermSynonyms(ontologyTerm){
		var synonyms = [];
		if(ontologyTerm.ontologyTermSynonym.length > 0){
			$.each(ontologyTerm.ontologyTermSynonym, function(index, ontologyTermSynonymEntity){
				if(ontologyTerm.ontologyTermName !== ontologyTermSynonymEntity.ontologyTermSynonym && $.inArray(ontologyTermSynonymEntity.ontologyTermSynonym, synonyms) === -1){
					synonyms.push(ontologyTermSynonymEntity.ontologyTermSynonym);
				}
			});
		}
		return synonyms;
	}
	
}($, window.top.molgenis = window.top.molgenis || {}));