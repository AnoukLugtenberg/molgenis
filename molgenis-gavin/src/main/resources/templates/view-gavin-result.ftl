<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">
<#assign css=[]>
<#assign js=["gavin-result.js"]>
<@header css js/>

<div class="row" id="gavin-view">
    <div class="col-md-12">
        <div class="panel panel-primary" id="instant-import">
            <div class="panel-heading">
                <h4 class="panel-title">
                    Gavin annotation ${jobExecution.filename}
                </h4>
            </div>
            <div class="panel-body">
                <div id="instant-import-alert"></div>

            <#if jobExecution.status == 'RUNNING' || jobExecution.status == 'PENDING'>
            <#-- Job running, show progress -->
                <div id="gavin-job" data-execution-id="${jobExecution.identifier}"></div>
            <#else>
            <#-- Job finished -->
                <h4>Input</h4>
                <p>Input contained:</p>
                <ul>
                    <#if jobExecution.comments?? && jobExecution.comments gt 0 >
                        <li>${jobExecution.comments} comment lines</li></#if>
                    <#if jobExecution.cadds?? && jobExecution.cadds gt 0 >
                        <li>${jobExecution.cadds} valid CADD lines</li></#if>
                    <#if jobExecution.vcfs?? && jobExecution.vcfs gt 0 >
                        <li>${jobExecution.vcfs} valid VCF lines</li></#if>
                    <#if jobExecution.errors?? && jobExecution.errors gt 0 >
                        <li>${jobExecution.errors} error lines.
                            <#if errorFileExists> <a href="/plugin/gavin-app/error/${jobExecution.identifier}">These
                                lines</a> couldn't be
                                parsed.
                            <#else>Error file no longer available. Job results are removed nightly.
                            </#if>
                        </li></#if>
                </ul>
                <#if jobExecution.status == 'SUCCESS'>
                    <h4>Download</h4>
                    <#if downloadFileExists><a
                            href="/plugin/gavin-app/download/${jobExecution.identifier}">Download ${jobExecution.filename}</a>
                    <#else>Download file no longer available. Job results are removed nightly.
                    </#if>
                </#if>
                <#if jobExecution.log??>
                    <h4>Execution log</h4>
                    <pre class="pre-scrollable">${jobExecution.log?html}</pre>
                </#if>

                <a class="btn btn-info" type="button" href="..">All done! Return to Gavin Upload page</a>
            </#if>
                <button class="btn btn-info" type="button" data-toggle="collapse" data-target="#link">Show link to this
                    page
                </button>
                <div class="collapse well" id="link">
                    <p>Make sure to keep this link if you want to view your results later.
                        They will remain available for 24 hours.</p>
                    <a href="${pageUrl}">${pageUrl}</a>
                </div>
            </div>
        </div>
    </div>
</div>

<@footer/>