<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#assign css=['mapping-service.css']>
<#assign js=['attribute-mapping.js', 'd3.min.js','vega.min.js','jstat.min.js', 'biobankconnect-graph.js', '/jquery/scrollTableBody/jquery.scrollTableBody-1.0.0.js', 'bootbox.min.js', 'jquery.ace.js']>

<@header css js/>

<script src="<@resource_href "/js/ace/src-min-noconflict/ace.js"/>" type="text/javascript" charset="utf-8"></script>
<script src="<@resource_href "/js/ace/src-min-noconflict/ext-language_tools.js"/>" type="text/javascript" charset="utf-8"></script>

<#if attributeMapping.sourceAttributeMetaData??>
	<#assign selected = attributeMapping.sourceAttributeMetaData.name>
<#else>
	<#assign selected = "null">
</#if>

<div class="row">
	<div class="col-md-12">
		<a href="${context_url}/mappingproject/${mappingProject.identifier}" class="btn btn-default btn-xs">
			<span class="glyphicon glyphicon-chevron-left"></span> Back to project
		</a>
		
		<hr></hr>
	</div>
</div>

<div class="row">
	<div class="col-md-12">
		<h4>Mapping from <i>${entityMapping.sourceEntityMetaData.name}</i> to <i>${entityMapping.targetEntityMetaData.name?html}.${attributeMapping.targetAttributeMetaData.label?html}</i>.</h4>
		${(attributeMapping.targetAttributeMetaData.description!"")?html}
	</div>
</div>

<div class="row">
	<div class="col-md-5">
		<div class="pull-left">
			<#if showSuggestedAttributes?c == "true">
				<h5>Source Attributes suggested by semantic search</h5>
			<#else>
				<h5>Source all attributes</h5>
			</#if>
		</div>
		<div class="pull-right">
			<form method="get" action="${context_url}/attributeMapping">
				<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
				<input type="hidden" name="target" value="${entityMapping.targetEntityMetaData.name?html}"/>
				<input type="hidden" name="source" value="${entityMapping.name?html}"/>
				<input type="hidden" name="targetAttribute" value="${attributeMapping.targetAttributeMetaData.name?html}"/>
				<input type="hidden" name="showSuggestedAttributes" value="${showSuggestedAttributes?string("false", "true")}"/>
				<div class="btn-group" role="group">
					<button id="reload-attribute-mapping-table" type="submit" class="btn btn-default" ">
						<#if showSuggestedAttributes?c == "true">
							Show all attributes
						<#else>
							Show only attributes suggested by semantic search
						</#if>
					</button>
				</div>
			</form>
		</div>
	</div>
</div>

<div class="row">
	<div class="col-md-5">
		<div id="attribute-table-container">
			<table id="attribute-mapping-table" class="table table-bordered scroll">
				<thead>
					<tr>
						<th>Attribute</th>
						<#if attributeMapping.targetAttributeMetaData.dataType == "xref" || attributeMapping.targetAttributeMetaData.dataType == "categorical">
							<th>Advanced editor</th>
						</#if>
					</tr>
				</thead>
				<tbody>
					<#list entityMapping.sourceEntityMetaData.attributes as source>
						<tr>
							<td class="${source.name}">
								<b>${source.label?html}</b> (${source.dataType})
								<#if source.nillable> <span class="label label-warning">nillable</span></#if>
								<#if source.unique> <span class="label label-default">unique</span></#if>
								<#if source.description??><br />${source.description?html}</#if>
								<#if hasWritePermission><button type="button" class="btn btn-default btn-xs toggle-btn not-selected pull-right" data-attribute="${source.name}"><span class="glyphicon glyphicon-ok"></span></button></#if>
							</td>
							
							<#--If the target is an xref/categorical and the source attribute is an xref/categorical/string-->
							<#if attributeMapping.targetAttributeMetaData.dataType == "xref" || attributeMapping.targetAttributeMetaData.dataType == "categorical">
								<td>	
								<#if source.dataType == "xref" || source.dataType == "categorical" || source.dataType == "string">
									<form method="post" action="${context_url}/advancedmappingeditor">
										<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
										<input type="hidden" name="target" value="${entityMapping.targetEntityMetaData.name?html}"/>
										<input type="hidden" name="source" value="${entityMapping.name?html}"/>
										<input type="hidden" name="targetAttribute" value="${attributeMapping.targetAttributeMetaData.name?html}"/>
										<input type="hidden" name="sourceAttribute" value="${source.name}"/>
								
										<button class="btn btn-default advanced-mapping-edit-btn"><span class="glyphicon glyphicon-list-alt"></span></button>
									</form>	
								</#if>
								</td>
							</#if>							
						</tr>
					</#list>
				</tbody>
			</table>
		</div>
			
		<form id="preview-mapping-result-form" method="POST" action="${context_url}/attributemappingfeedback">
			<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
			<input type="hidden" name="target" value="${entityMapping.targetEntityMetaData.name?html}"/>
			<input type="hidden" name="source" value="${entityMapping.name?html}"/>
			<input type="hidden" name="targetAttribute" value="${attributeMapping.targetAttributeMetaData.name?html}"/>
			<input type="hidden" name="algorithm" value="${(attributeMapping.algorithm!"")?html}"/>
			
			<input type="submit" style="display:none;"></input>
		</form>
			
		<form id="save-attribute-mapping-form" method="POST" action="${context_url}/saveattributemapping">
			<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
			<input type="hidden" name="target" value="${entityMapping.targetEntityMetaData.name?html}"/>
			<input type="hidden" name="source" value="${entityMapping.name?html}"/>
			<input type="hidden" name="targetAttribute" value="${attributeMapping.targetAttributeMetaData.name?html}"/>
			<input type="hidden" name="algorithm" value="${(attributeMapping.algorithm!"")?html}"></input>				

			<input type="submit" style="display:none;"></input>
		</form>  
					
		<legend>Generated Mapping</legend>
		<strong>
			<p>Target attribute: ${attributeMapping.targetAttributeMetaData.name?html}</p> 
			<p>Source mapping algorithm: <mark id="algorithm-container"><em><#if attributeMapping.algorithm??>${(attributeMapping.algorithm!"")?html}</#if></em></mark></p>			
		</strong>
		
		<button id="preview-mapping-result-btn" type="btn" value="Submit" class="btn btn-success">Preview mapping result</button>
		<#if hasWritePermission>
			<button id="save-attribute-mapping-btn" type="btn" value="Submit" class="btn btn-primary">Save mapping</button>
		</#if>
	</div>
	
	<div id="algorithm-results-container" class="col-md-7">
		<ul class="nav nav-tabs" role="tablist">
    		<li role="presentation" class="active"><a href="#preview" aria-controls="preview" role="tab" data-toggle="tab">Preview</a></li>
    		<li role="presentation"><a href="#script" aria-controls="script" role="tab" data-toggle="tab">Script</a></li>
    		<li role="presentation"><a href="#statistics" aria-controls="statistics" role="tab" data-toggle="tab">Statistics</a></li> 
   		</ul>
		
		 <div class="tab-content">
    		<div role="tabpanel" class="tab-pane active" id="preview"><@preview /></div>
    		<div role="tabpanel" class="tab-pane" id="script"><@script /></div>
    		<div role="tabpanel" class="tab-pane" id="statistics"><@statistics /></div>
    	</div>
	</div>
</div>


<#macro preview>
	<div class="row">
		<div class="col-md-12">
			<div id="mapping-preview-container">
				<table id="mapping-preview-table" class="table"></table>
			</div>
		</div>
	</div>
</#macro>

<#macro script>
	<div class="row">
		<div class="col-md-12">
			<h3>Algorithm</h3>
			<form id="save-attribute-mapping-form" method="POST" action="${context_url}/saveattributemapping">
				<textarea class="form-control" name="algorithm" rows="15" id="edit-algorithm-textarea" <#if !hasWritePermission>data-readonly="true"</#if> width="100%">${(attributeMapping.algorithm!"")?html}</textarea>
				<hr />
				<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
				<input type="hidden" name="target" value="${entityMapping.targetEntityMetaData.name?html}"/>
				<input type="hidden" name="source" value="${entityMapping.name?html}"/>
				<input type="hidden" name="targetAttribute" value="${attributeMapping.targetAttributeMetaData.name?html}"/>
				<button type="button" class="btn btn-primary" id="btn-test">Test</button>
				<#if hasWritePermission>
					<button type="submit" class="btn btn-primary">Save</button> 
					<button type="reset" class="btn btn-warning">Reset</button>
				</#if>			
			</form>
		</div>
	</div>
</#macro>

<#macro statistics>
	<div class="row">
		<div class="col-md-12">
			<div id="statistics-container">				
				<center><legend>Summary statistics</legend></center>
				<table class="table table-bordered">
						<tr><th>Total cases</th><td id="stats-total"></td></tr>
						<tr><th>Valid cases</th><td id="stats-valid"></td></tr>
						<#switch attributeMapping.targetAttributeMetaData.dataType>
						<#case "long">
						<#case "decimal">
						<#case "int">
							<tr><th>Mean</th><td id="stats-mean"></td></tr>
							<tr><th>Median</th><td id="stats-median"></td></tr>
							<tr><th>Standard deviation</th><td id="stats-stdev"></td></tr>
					</#switch>
				</table>
			
				<#switch attributeMapping.targetAttributeMetaData.dataType>
					<#case "long">
					<#case "decimal">
					<#case "int">
						<div class="col-md-6">
							<center><legend>Distribution plot</legend></center>
							<div class="distribution">
							</div>
						</div>
				</#switch>
			</div>
		</div>
	</div>
</#macro>

<@footer/>