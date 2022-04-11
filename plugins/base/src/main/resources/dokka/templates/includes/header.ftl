<#macro display>
<div class="navigation-wrapper" id="navigation-wrapper">
    <div id="leftToggler"><span class="icon-toggler"></span></div>
    <div class="library-name">
        <@template_cmd name="pathToRoot">
            <a href="${pathToRoot}index.html">
                <@template_cmd name="projectName">
                    <span>${projectName}</span>
                </@template_cmd>
            </a>
        </@template_cmd>
    </div>
    <div>
        <#-- This can be handled by a versioning plugin -->
        <@version/>
    </div>
    <div class="pull-right d-flex">
        <#if sourceSets??>
            <div class="filter-section" id="filter-section">
                <#list sourceSets as ss>
                    <button class="platform-tag platform-selector ${ss.platform}-like" data-active="" data-filter="${ss.filter}">${ss.name}</button>
                </#list>
            </div>
        </#if>
        <button id="theme-toggle-button"><span id="theme-toggle"></span></button>
        <div id="searchBar"></div>
    </div>
</div>
</#macro>