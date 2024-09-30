<#macro display>
    <#if sourceSets?has_content>
        <div class="filter-section filter-section_loading" id="filter-section">
            <#list sourceSets as ss>
                <button class="platform-tag platform-selector ${ss.platform}-like" data-active=""
                        data-filter="${ss.filter}">${ss.name}</button>
            </#list>
            <div class="dropdown filter-section--dropdown" data-role="dropdown" id="filter-section-dropdown">
                <button class="button button_dropdown filter-section--dropdown-toggle" role="combobox"
                        data-role="dropdown-toggle"
                        aria-controls="platform-tags-listbox"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                        aria-label="Toggle source sets"
                ></button>
                <ul role="listbox" id="platform-tags-listbox" class="dropdown--list" data-role="dropdown-listbox">
                    <div class="dropdown--header"><span>Platform filter</span>
                        <button class="button" data-role="dropdown-toggle" aria-label="Close platform filter">
                            <i class="ui-kit-icon ui-kit-icon_cross"></i>
                        </button>
                    </div>
                    <#list sourceSets as ss>
                        <li role="option" class="dropdown--option platform-selector-option ${ss.platform}-like" tabindex="0">
                            <label class="checkbox">
                                <input type="checkbox" class="checkbox--input" id="${ss.filter}"
                                       data-filter="${ss.filter}"/>
                                <span class="checkbox--icon"></span>
                                ${ss.name}
                            </label>
                        </li>
                    </#list>
                </ul>
                <div class="dropdown--overlay"></div>
            </div>
        </div>
    </#if>
</#macro>
