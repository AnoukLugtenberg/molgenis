<#include "molgenis-header.ftl">
<#include "molgenis-footer.ftl">

<#assign css=["apps.css"]>
<#assign js=["apps.js"]>

<@header css js/>
<div class="row">
    <div class="col-md-12">
    <#if appNotAvailableMessage??>
        <script>
            alert("App not available!");
        </script>
    </#if>
        <button class="btn btn-default" id="create-app-btn">Create new App</button>
    </div>
    <hr></hr>
</div>
<div class="row">
<#list apps as app>
    <div class="col-sm-4 col-md-2">
        <div class="thumbnail">
            <div class="well">
                <#if app.active>
                    <a target="_blank" href="/menu/main/apps/${app.name}"><img src=<#if (app.appiconurl)??>${app.appiconurl}<#else>"http://www.cheatsheet.com/wp-content/uploads/2012/04/apple_app_store.jpeg"</#if> alt="App icon" height="100px" width="100px"></a>
                <#else>
                    <div class="inactive"><img src=<#if (app.appiconurl)??>${app.appiconurl}<#else>"http://www.cheatsheet.com/wp-content/uploads/2012/04/apple_app_store.jpeg"</#if> alt="App icon" height="100px" width="100px"></div>
                </#if>
            </div>
            <div class="caption">
                <h3>${app.name}</h3>
                <p>${app.description}</p>
            </div>

            <#if !app.active>
                <button class="btn btn-success activate-app-btn" data-appname="${app.name}">Activate!</button>
            <#else>
                <button class="btn btn-danger deactivate-app-btn" data-appname="${app.name}">DeActivate!</button>
            </#if>
        </div>
    </div>
</#list>
</div>

<div id="create-app-form"></div>

<@footer/>