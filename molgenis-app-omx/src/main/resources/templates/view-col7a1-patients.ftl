<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#assign css=[
	"jquery.bootstrap.wizard.css",
	"bootstrap-datetimepicker.min.css",
	"ui.fancytree.min.css",
	"jquery-ui-1.9.2.custom.min.css",
	"jquery.molgenis.table.css",
	"select2.css",
	"iThing-min.css",
	"bootstrap-switch.min.css",
	"dataexplorer.css"]>
<#assign js=[
	"jquery-ui-1.9.2.custom.min.js",
	"jquery.bootstrap.wizard.min.js",
	"bootstrap-datetimepicker.min.js",
	"dataexplorer-filter.js",
	"dataexplorer-filter-dialog.js",
	"dataexplorer-filter-wizard.js",
	"jquery.fancytree.min.js",
	"jquery.molgenis.tree.js",
	"select2.min.js",
	"jQEditRangeSlider-min.js",
	"bootstrap-switch.min.js",
	"jquery.molgenis.xrefsearch.js",
	"dataexplorer.js",
	"jquery.molgenis.table.js",
	"col7a1-patients.js"]>	
<@header css js/>
	<form id="form-col7a1-patients" method="get" action="${context_url}/import">
		<div class="row-fluid">
			Patients!!
			<div class="row-fluid data-table-container" id="data-table-container"></div>
				<div id="dataexplorer-grid-data" class="row-fluid data-table-pager-container">
					<table class="table-striped table-condensed molgenis-table">
						<thead>
							<#list headers as header>
								<th>${header}</th>
							</#list>
						</thead>
						<tbody>
							<#list rows as row>
								<tr>
									<#list row.cells as cell>
										<td>
											<#list cell.values as value>
												<div>${value.value}</div>
											</#list>
										</td>
									</#list>
								</tr>
							</#list>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</form>
<@footer/>
