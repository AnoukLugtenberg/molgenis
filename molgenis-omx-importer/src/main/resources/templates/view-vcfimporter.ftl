<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=[]>
<#assign js=["additional-methods.min.js", "vcfimporter.js"]>
<@header css js/>
<form name="vcf-importer-form" class="form-horizontal" action="${context_url}/import" method="POST" enctype="multipart/form-data">
	<div class="control-group">
		<label class="control-label" for="file">VCF file *</label>
		<div class="controls">
			<input type="file" id="file" name="file" required>
			<span class="help-inline">Accepted formats are vcf and vcf.gz.</span>
		</div>
	</div>
	<div class="control-group">
		<label class="control-label" for="name">Dataset name *</label>
	    <div class="controls">
	      <input type="text" name="name" required>
	    </div>
	</div>
	<div class="control-group">
		<div class="controls">
			<button type="submit" class="btn btn-primary">Import</button>
		</div>
	</div> 
</form>
<@footer/>