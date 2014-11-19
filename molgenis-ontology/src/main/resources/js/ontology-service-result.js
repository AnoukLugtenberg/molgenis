(function($, molgenis) {
	"use strict";
	
	var ontologyServiceRequest = null;
	var result_container = null;
	var reserved_identifier_field = 'Identifier';
	
	molgenis.OntologySerivce = function OntologySerivce(container, request){
		result_container = container;
		ontologyServiceRequest = request;
	};
	
	molgenis.OntologySerivce.prototype.updatePageFunction = function(page){
		
		ontologyServiceRequest['entityPager'] = {
			'start' : page.start,
			'num' : page.end - page.start
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
					var slimDiv = $('<div style="width:96%;margin-left:2%;"></div>').appendTo(result_container);
					slimDiv.append('<p style="font-size:20px;margin-top:-20px;"><strong>' + (ontologyServiceRequest.matched ? 'Matched result' : 'Unmatched result') + '</strong></p>');
					var table = $('<table align="center"></table>').addClass('table').appendTo(slimDiv);
					$('<tr />').append('<th style="width:40%;">Input term</th><th style="width:40%;">Matched term</th><th style="width:10%;">Score</th><th>Validate</th>').appendTo(table);
					$.each(data.items, function(index, entity){
						table.append(createRowForMatchedTerm(entity, ontologyServiceRequest.matched));
					});
				}else{
					result_container.append('<center>There are not results!</center>');
				}
			}
		});
	};
	
	function createRowForMatchedTerm(entity, validated){
		var row = $('<tr />');
		row.append(gatherInputInfoHelper(entity.inputTerm));
		row.append(gatherOntologyInfoHelper(entity.inputTerm, entity.ontologyTerm));
		$('<td />').append(entity.matchedTerm.Score.toFixed(2) + '%').appendTo(row);
		if(validated){
			$('<td />').append('<span class="glyphicon glyphicon-ok"></span>').appendTo(row);
		}else{
			var button = $('<button class="btn btn-default" type="button">Validate</button>');
			$('<td />').append(button).appendTo(row);
			button.click(function(){
				$.ajax({
					type : 'POST',
					url : molgenis.getContextUrl() + '/match/entity',
					async : false,
					data : JSON.stringify({'Identifier' : entity.inputTerm.Identifier, 'entityName' : ontologyServiceRequest.entityName}),
					contentType : 'application/json',
					success : function(data) {
						createTableForCandidateMappings(entity.inputTerm, data, row);
					}
				});
			});
		}
		return row;
	}
	
	function createTableForCandidateMappings(inputEntity, data, row){
		var container = $('<div class="row"></div>').css({'margin-bottom':'20px'});
		row.parents('table:eq(0)').hide();
		row.parents('div:eq(0)').append(container);
		if(data.ontologyTerms && data.ontologyTerms.length > 0){
			var backButton = $('<button type="button" class="btn btn-default">Go back</button>').css({'margin-bottom':'10px','float':'right'});
			var hintInformation = $('<center><p style="font-size:15px;">The candidate ontology terms are sorted based on similarity score, please select one of them by clicking <span class="glyphicon glyphicon-ok"></span> button</p></center>');
			var table = $('<table class="table"></table>').append('<tr><th style="width:40%;">Input Term</th><th style="width:40%;">Candidate mapping</th><th style="width:10%;">Score</th><th>Select</th></tr>');
			var count = 0;
			$.each(data.ontologyTerms, function(index, ontologyTerm){
				if(count >= 10) return;
				var row = $('<tr />').appendTo(table);
				row.append(count == 0 ? gatherInputInfoHelper(inputEntity) : '<td></td>');
				row.append(gatherOntologyInfoHelper(inputEntity, ontologyTerm));
				row.append('<td>' + ontologyTerm.Score.toFixed(2) + '%</td>');
				row.append('<td><button type="button" class="btn btn-default"><span class="glyphicon glyphicon-ok"></span></button></td>');
				row.find('button:eq(0)').click(function(){
					$.ajax({
						type : 'POST',
						url : molgenis.getContextUrl() + '/match/validate',
						async : false,
						data : JSON.stringify({'Identifier' : inputEntity.Identifier, 'entityName' : ontologyServiceRequest.entityName, 'ontologyTermIRI' : ontologyTerm.ontologyTermIRI, 'Score' : ontologyTerm.Score}),
						contentType : 'application/json',
						success : function(data) {
							container.parents('form:eq(0)').attr({
								'action' : molgenis.getContextUrl() + '/result/' + ontologyServiceRequest.entityName,
								'method' : 'GET'
							}).submit();
						}
					});
				});
				count++;
			});
			backButton.click(function(){
				container.remove();
				row.parents('table:eq(0)').show();
			});
			$('<div class="col-md-12"></div>').append(hintInformation).append(backButton).append(table).appendTo(container);
		}else{
			container.append('<center>There are no candidate mappings for this input term!</center>');
		}
	}
	
	function gatherInputInfoHelper(inputTerm){
		var inputTermTd = $('<td />');
		$.map(inputTerm ? inputTerm : {}, function(val, key){
			if(key !== reserved_identifier_field) inputTermTd.append('<div>' + key + ' : ' + val + '</div>');
		});
		return inputTermTd;
	}
	
	function gatherOntologyInfoHelper(inputEntity, ontologyTerm){
		var ontologyTermTd = $('<td />').append('<div>Name : <a href="' + ontologyTerm.ontologyTermIRI + '" target="_blank">' + 
				ontologyTerm.ontologyTerm + '</a></div>').append('<div>Synonym : ' + (ontologyTerm.ontologyTermSynonym !== ontologyTerm.ontologyTerm ? ontologyTerm.ontologyTermSynonym : 'N/A') + '</div>');
		$.each(Object.keys(inputEntity), function(index, key){
			if(key.toLowerCase() !== 'name' && key.toLowerCase().search('synonym') === -1 && key.toLowerCase() !== reserved_identifier_field.toLowerCase()){
				ontologyTermTd.append('<div>' + key + ' : ' + (ontologyTerm[key] ? ontologyTerm[key] : 'N/A')  + '</div>');
			}
		});
		return ontologyTermTd;
	}
	
}($, window.top.molgenis = window.top.molgenis || {}));