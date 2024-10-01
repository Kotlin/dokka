<#macro display>
    <#if sourceSets?has_content>
        <div class="filter-section" id="filter-section">
            <#list sourceSets as ss>
                <button class="platform-tag platform-selector ${ss.platform}-like" data-active=""
                        data-filter="${ss.filter}">${ss.name}</button>
            </#list>
            <#list sourceSets as ss>
                <button class="platform-tag platform-selector ${ss.platform}-like" data-active=""
                        data-filter="${ss.filter}">2 ${ss.name}</button>
            </#list>
            <#list sourceSets as ss>
                <button class="platform-tag platform-selector ${ss.platform}-like" data-active=""
                        data-filter="${ss.filter}">3 ${ss.name}</button>
            </#list>
            <div class="dropdown" data-role="dropdown" id="filter-section-dropdown">
                <button class="button button_dropdown filter-section--dropdown-toggle" role="combobox"
                        data-role="dropdown-toggle"
                        aria-controls="platform-tags-listbox"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                        aria-label="Toggle source sets"
                ></button>
                <ul role="listbox" id="platform-tags-listbox" class="dropdown--list" data-role="dropdown-listbox">
                    <div class="filter-section--header"><span>Platform filter</span>
                        <button class="button" data-role="dropdown-toggle" aria-label="Close platform filter">
                            <i class="ui-kit-icon ui-kit-icon_cross"></i>
                        </button>
                    </div>
                    <#list sourceSets as ss>
                        <li role="option" class="dropdown--option" onclick="onToggleOption(event)"
                            onkeyup="onToggleOptionByKey(event)" tabindex="0">
                            <input type="checkbox" class="checkbox dropdown--checkbox" id="${ss.filter}"
                                   data-filter="${ss.filter}" tabindex="-1"/>
                            ${ss.name}
                        </li>
                    </#list>
                    <#list sourceSets as ss>
                        <li role="option" class="dropdown--option" onclick="onToggleOption(event)"
                            onkeyup="onToggleOptionByKey(event)" tabindex="0">
                            <input type="checkbox" class="checkbox dropdown--checkbox" id="1${ss.filter}"
                                   data-filter="${ss.filter}" tabindex="-1"/>
                            2 ${ss.name}
                        </li>
                    </#list>
                    <#list sourceSets as ss>
                        <li role="option" class="dropdown--option" onclick="onToggleOption(event)"
                            onkeyup="onToggleOptionByKey(event)" tabindex="0">
                            <input type="checkbox" class="checkbox dropdown--checkbox" id="2${ss.filter}"
                                   data-filter="${ss.filter}" tabindex="-1"/>
                            3 ${ss.name}
                        </li>
                    </#list>
                </ul>
                <div class="filter-section--overlay" id="filter-section-overlay"></div>
            </div>
        </div>
    </#if>
</#macro>
