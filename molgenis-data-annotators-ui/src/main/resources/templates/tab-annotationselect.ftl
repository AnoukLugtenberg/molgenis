<#macro annotationselect_panel>
	<#--Panel 4: Annotation tool / database selection-->
	<div class="tab-pane" id="tab4">
		<form id="execute-annotation-app" role="form" action="${context_url}/execute-annotation-app" method="post">
			  
			<h4>Your selected dataset: <div class=lead id="selected-dataset-name"></div></h4> 
			
			<div class="form-group">
				<div class="control">
					<div class="row-fluid">
						<div class="span1"></div>
						<div class="span5">
							<h5>Annotations available</h5>
							
							<hr></hr>
							
							<div id="annotator-checkboxes-enabled"></div>
					
						</div>
						<div class="span5">
							<h5>Annotations not available
								<a id="disabled-tooltip" data-toggle="tooltip" data-placement="top-right" 
									title= "These annotations are not available for the selected data set because the data set does not contain the correct data"> 
										
									<span class="icon icon-question-sign"></span>
								</a>
							</h5> 
							
							<hr></hr>
							
							<div id="annotator-checkboxes-disabled"></div>
						</div>
					</div>		
				</div>	
			</div>
			
			<input type="hidden" value="" id="dataset-identifier" name="dataset-identifier"></input>
				
		</form>	
	</div>
</#macro>