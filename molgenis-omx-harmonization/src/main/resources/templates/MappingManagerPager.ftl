<form id="wizardForm" name="wizardForm" method="post" class="form-horizontal" action="">
	<div class="row-fluid">
		<div class="span12">
			<button id="downloadButton" class="btn btn-primary float-right">Download</button>
		</div>
	</div>
	<div class="row-fluid">
		<div class="span12">
			<div class="row-fluid">
				<div class="span12">
					<div id="data-table-container" class="row-fluid data-table-container">
						<table id="dataitem-table" class="table table-striped table-condensed show-border">
						</table>
					</div>
					<div class="pagination pagination-centered">
						<ul id="table-papger"></ul>
					</div>
				</div>
			</div>
		</div>
	</div>
	<script type="text/javascript">
		$(document).ready(function(){
			var molgenis = window.top.molgenis;
			molgenis.setContextURL('${context_url}');
			molgenis.getMappingManager().changeDataSet(${wizard.selectedDataSet.id?c});
			$('#downloadButton').click(function(){
				molgenis.getMappingManager().downloadMappings();
				return false;
			});
		});
	</script>
</form>