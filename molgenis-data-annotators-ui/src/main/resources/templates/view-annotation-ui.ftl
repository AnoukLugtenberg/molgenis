<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#-- seperate ftl's for every tab panel are included here-->
<#include "tab-annotationselect.ftl">
<#include "tab-filterbyregion.ftl">
<#include "tab-filterbyphenotype.ftl">
<#include "tab-fileupload.ftl">

<#assign css=["annotate-ui.css", "jquery-ui-1.10.3.custom.css", "chosen.css", "bootstrap-fileupload.css"]>
<#assign js=["annotation.ui.js", "jquery-ui-1.10.3.custom.min.js", "chosen.jquery.min.js", "bootstrap-fileupload.js", "jquery.bootstrap.wizard.js"]>

<@header css js />

<div class="row-fluid">
	<div class="span12">
		<div id="rootwizard" class="tabbable tabs-left">

			<ul>
			  	<li><a href="#tab1" class="tab1" data-toggle="tab">Select or Upload a Dataset</a></li>
				<li><a href="#tab2" class="tab2" data-toggle="tab">Filter by: Genome</a></li>
				<li><a href="#tab3" class="tab3" data-toggle="tab">Filter by: Phenotype</a></li>
				<li><a href="#tab4" class="tab4" data-toggle="tab">Select your annotation</a></li>
		
				<li>
					<button type="submit" form="execute-annotation-app" class="btn">Go</button>
				</li>
			</ul>
		
			<div class="tab-content" style="height:auto;">
				<#--panel code located in seperate ftl's for readability-->
		    	<@fileupload_panel />
		    	<@regionfilter_panel />
		    	<@phenotypefilter_panel />
		    	<@annotationselect_panel />			
			</div>
		</div>
	</div>
</div>

<@footer />
