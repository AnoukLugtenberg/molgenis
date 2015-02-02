<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#assign css=['mapping-service.css']>
<#assign js=['mapping-service.js']>

<@header css js/>
<@createNewMappingProjectModal />

<!--Table containing mapping projects-->
<div class="row">
	<div class="col-md-12">
		<div class="col-md-6">
			<h1>Mapping projects overview</h1>
			<p>Create and view mapping projects</p>
			
			<div class="btn-group" role="group">
				<a href="#" class="btn btn-primary" data-toggle="modal" data-target="#create-new-mapping-project-modal">Create a new mapping</a>  				
			</div>
			<div class="btn-group" role="group">
				<button type="button" class="btn btn-success">Edit</button>  				
			</div>
			
			<hr></hr>
		</div>
	</div>
</div>
<div class="row">
	<div class="col-md-12">
		<div class="col-md-6">
			<#if mappingProjects??>
				<table class="table table-bordered">
		 			<thead>
		 				<tr>
		 					<th>Mapping name</th>
		 					<th>Owner</th>
		 					<th>Target entities</th>
		 					<th>Mapped sources</th>
		 				</tr>
		 			</thead>
		 			<tbody>
		 				<#list mappingProjects as project>
		 				<tr>	 					
		 					<td><a href="${context_url}/mappingproject/${project.identifier}">${project.name?html}</a></td>
		 					<td>${project.owner.username?html}</td>
		 					<td>
		 					<#list project.mappingTargets as target>
		 						${target.name?html}<#if target_has_next>, </#if>
	 						</#list>
	 						</td>
		 					<td></td>	
		 				</tr>
		 				</#list>
		 			</tbody>
				</table>
			</#if>
		</div>
	</div>
</div>

<@footer/>

<#macro createNewMappingProjectModal>
	<div class="modal fade" id="create-new-mapping-project-modal" tabindex="-1" role="dialog" aria-labelledby="create-new-mapping-project-modal" aria-hidden="true">
		<div class="modal-dialog">
	    	<div class="modal-content">
	        	<div class="modal-header">
	        		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	        		<h4 class="modal-title" id="create-new-mapping-project-label">Create a new mapping project</h4>
	        	</div>
	        	<div class="modal-body">
      				<form id="create-new-mapping-project-form" method="post" action="${context_url}/addmappingproject">	
  						<div class="form-group">
		            		<label>Mapping project name</label>
		  					<input name="mapping-project-name" type="text" class="form-control" placeholder="Mapping name" required="required">
						</div>
					
						<hr></hr>	
						
						<div class="form-group">
							<label>Select the Target entity</label>
							<select name="target-entity" class="form-control" required="required" placeholder="Select a target entity">
		    					<#list entityMetaDatas as entityMetaData>
		    						<option value="${entityMetaData.name?html}">${entityMetaData.name?html}</option>
		    					</#list>
							</select>
						</div>
						
						<input type="submit" class="submit" style="display:none;">
					</form>
        		</div>
        		
	        	<div class="modal-footer">
	        		<button type="button" id="submit-new-mapping-project-btn" class="btn btn-primary">Create project</button>
	                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
	    		</div>	    				
    		</div>
		</div>
	</div>
</#macro>	