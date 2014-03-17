<div id="feature-select-container">
	<label class="control-label" for="feature-select">Aggregate by:</label>
	<div id="feature-select" class="controls">
	</div>
</div>
<div class="row-fluid data-table-container form-horizontal" id="dataexplorer-aggregate-data">
	<div id="aggregate-table-container"></div>
</div>
<script>
	$.when($.ajax("/js/dataexplorer-aggregates.js", {'cache': true}))
		.then(function() {
			molgenis.dataexplorer.aggregates.createAggregatesTable();
		});
</script>
