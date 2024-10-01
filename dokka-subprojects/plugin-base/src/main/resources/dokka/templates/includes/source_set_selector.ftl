<#macro display>
    <#if sourceSets?has_content>
        <div class="filter-section" id="filter-section">
            <#list sourceSets as ss>
                <button class="platform-tag platform-selector ${ss.platform}-like" data-active="" data-filter="${ss.filter}">${ss.name}</button>
            </#list>
            <button class="button button_dropdown" onclick="onToggleDropdown(event)"></button>
        </div>
    </#if>
</#macro>
