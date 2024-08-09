<#macro display>
    <div class="dropdown">
        <button class="button button_dropdown" role="combobox"
                aria-controls="id-of-listbox"
                aria-haspopup="listbox"
                aria-expanded="false"
        ></button>
        <ul role="listbox" id="id-of-listbox" class="dropdown--list">
                <li role="option" class="dropdown--option"  tabindex="0">
                    <input type="checkbox" class="checkbox dropdown--checkbox" id="option-1" tabindex="-1"/>
                    Option 1
                </li>
                <li role="option" class="dropdown--option"  tabindex="0">
                    <input type="checkbox" class="checkbox dropdown--checkbox" id="option-2" tabindex="-1"/>
                    Option 2
                </li>
            </#list>
        </ul>
    </div>
</#macro>