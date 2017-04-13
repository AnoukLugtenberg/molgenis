<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<@header css js/>

<button class="btn btn-primary" id="view-full-dataset-btn">View full dataset</button>
<hr>
<#include viewName+".ftl">

<@footer/>

<script>
    var entityName = '${entityName}'
    $('body').on('click', '#view-full-dataset-btn', function () {
        window.open(window.location.origin + molgenis.getContextUrl() + '?entity=' + entityName, '_self')
    })
</script>