<#include "resource-macros.ftl">
<div class="row">
        <div class="col-md-12">
            <div id="annotator-select-container">
            	<form id="annotate-dataset-form" role="form">
                    <div class="well">
                    <legend>Annotators available</legend>
                        <div class="row">
                            <div class="col-md-12">
                                <a href="#" class="btn btn-link pull-left select-all-btn">Select all</a>
                                <a href="#" class="btn btn-link pull-left deselect-all-btn">Deselect all</a>
                            </div>
                        </div>
                        <div id="annotator-checkboxes-enabled"></div>
            		<legend>Annotations not available
                        <a id="disabled-tooltip" data-toggle="tooltip"
                            title= "These annotations are not available for the selected data set because:
                            1) The annotation data is not available on the server, 2) A webservice might be offline or 3) Your data set does not contain the correct columns">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </legend>
                    <div id="annotator-checkboxes-disabled"></div>
            		</div>
					<input type="hidden" value="" id="dataset-identifier" name="dataset-identifier">
					<button id="annotate-dataset-button" class="btn btn-default">Run annotation</button>
					<!--
					Annotating to a new repository is currently not possible pending story:
                    #2983: As annotator use I'd like to write annotate results to a new repository
					-->
                    <!--<input type="checkbox" name="createCopy"> Copy before annotating-->
            	</form>
            </div>
    </div>
</div>
<script>
	$.when($.ajax("<@resource_href "/js/dataexplorer-annotators.js"/>", {'cache': true}))
		.then(function() {
			molgenis.dataexplorer.annotators.getAnnotatorSelectBoxes();		
		});

    $('.select-all-btn').click(function(e) {
        $("input[name='annotatorNames']").each(function() {
            this.checked = true;
        });
    });

    $('.deselect-all-btn').click(function(e) {
        $("input[name='annotatorNames']").each(function() {
            this.checked = false;
        });
    });
</script>