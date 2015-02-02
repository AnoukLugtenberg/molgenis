<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#assign css=['mapping-service.css']>
<#assign js=['mapping-service.js']>

<@header css js/>

<div class="row">
	<div class="col-md-12">
		<a href="${context_url}" class="btn btn-danger btn-xs"><span class="glyphicon glyphicon-chevron-left"></span>    Back to mapping project overview</a>	
	</div>
</div>

<div class="row">
	<div class="col-md-6">
		<h3>Mappings for the ${mappingProject.name?html} project</h3>
		<p>Create and view mappings.</p>
		
	</div>
</div>

<div class="row">
	<div class="col-md-11">
		<table class="table table-bordered scroll">
 			<thead>
 				<tr>
 					<th>Target model: ${selectedTarget}</th>
				<#list mappingProject.getMappingTarget(selectedTarget).entityMappings as source>
					<th>
						<form method="post" action="${context_url}/removeentitymapping">
							<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
							<input type="hidden" name="target" value="${selectedTarget}"/>
							<input type="hidden" name="source" value="${source.name}"/>
							Source: ${source.name} <button type="submit" class="btn btn-danger btn-xs pull-right"><span class="glyphicon glyphicon-minus"></span></button>
						</form>	
					</th>
				</#list>
 				</tr>
 			</thead>
 			<tbody>
				<#list mappingProject.getMappingTarget(selectedTarget).target.attributes as attribute>
					<tr>
						<td>
							<b>${attribute.name?html}</b><#if attribute.description??><br />test${attribute.description?html}</#if>
						</td>
						<#list mappingProject.getMappingTarget(selectedTarget).entityMappings as source>
							<td>
								<form method="get" action="${context_url}/editattributemapping">
									<#if source.getAttributeMapping(attribute.name)??>
										${source.getAttributeMapping(attribute.name).sourceAttributeMetaData.name}
									</#if>
									<button type="submit" class="btn btn-primary btn-xs pull-right">
										<span class="glyphicon glyphicon-pencil"></span>
									</button>
									<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}"/>
									<input type="hidden" name="target" value="${selectedTarget}"/>
									<input type="hidden" name="source" value="${source.name}"/>
									<input type="hidden" name="attribute" value="${attribute.name}"/>
									
								</form>
							</td>
						</#list>
					</tr>
				</#list>
			</tbody>
		</table>
	</div>
	<div class="col-md-1">
		<a id="add-new-attr-mapping-btn" href="#" class="btn btn-success btn-xs" data-toggle="modal" data-target="#create-new-source-column-modal"><span class="glyphicon glyphicon-plus"></span>Add source</a>
	</div>
</div>

<div class="row">
	<div class="col-md-12">
		<a id="add-new-attr-mapping-btn" href="#" class="btn btn-primary btn-xs" data-toggle="modal" data-target="#create-integrated-entity-modal">Create integrated dataset</a>
	</div>
</div>

<!--Create new source dialog-->
<div class="modal" id="create-new-source-column-modal" tabindex="-1" role="dialog">
	<div class="modal-dialog">
    	<div class="modal-content">
        	<div class="modal-header">
        		<button type="button" class="close" data-dismiss="modal">&times;</button>
        		<h4 class="modal-title" id="create-new-source-column-modal-label">Create a new mapping project</h4>
        	</div>
        	<div class="modal-body">	
        		<form id="create-new-source-form" method="post" action="${context_url}/addentitymapping">	
					<div class="form-group">
	            		<label>Select a new source to map against the target attribute</label>
  						<select name="source" class="form-control" required="required" placeholder="Select a target entity">
	    					<#list entityMetaDatas as entityMetaData>
    							<option value="${entityMetaData.name?html}">${entityMetaData.name?html}</option>
	    					</#list>
						</select>
					</div>
					
					<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}">
					<input type="hidden" name="target" value="${selectedTarget}">
					<input type="submit" class="submit" style="display:none;">
				</form>
        	
    		</div>
    		
        	<div class="modal-footer">
        		<button type="button" id="submit-new-source-column-btn" class="btn btn-primary">Add source</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
    		</div>	    				
		</div>
	</div>
</div>

<!--Create integrated entity dialog-->
<div class="modal" id="create-integrated-entity-modal" tabindex="-1" role="dialog">
	<div class="modal-dialog">
    	<div class="modal-content">
        	<div class="modal-header">
        		<button type="button" class="close" data-dismiss="modal">&times;</button>
        		<h4 class="modal-title" id="create-integrated-entity-modal-label">Create a new mapping project</h4>
        	</div>
        	<div class="modal-body">	
        		<form id="create-integrated-entity-form" method="post" action="${context_url}/createintegratedentity">
        			
        			<label>Enter a name for the integrated dataset</label>
        			<input name="newEntityName" type="text" value="" required></input>
        		
        			<input type="hidden" name="mappingProjectId" value="${mappingProject.identifier}">
        			<input type="hidden" name="target" value="${selectedTarget}">
        			
        			<input type="submit" class="submit" style="display:none;">
				</form>
    		</div>
    		
        	<div class="modal-footer">
        		<button type="button" id="create-integrated-entity-btn" class="btn btn-primary">Create integrated dataset</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
    		</div>	    				
		</div>
	</div>
</div>

