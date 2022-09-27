<#macro display>
    <#if sourceSets??>
        <div class="filter-section" id="filter-section">
            <#list sourceSets as ss>
                <button class="platform-tag platform-selector ${ss.platform}-like" data-active="" data-filter="${ss.filter}">${ss.name}</button>
            </#list>
        </div>
    </#if>
</#macro>