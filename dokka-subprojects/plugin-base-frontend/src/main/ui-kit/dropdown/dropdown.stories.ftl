<#macro display>
    <div class="dropdown">
        <button class="button button_dropdown" role="combobox"
                aria-controls="id-of-listbox"
                aria-haspopup="listbox"
                aria-expanded="false"
        ></button>
        <ul role="listbox" id="id-of-listbox" class="dropdown--list">
            <li role="option" class="dropdown--option" tabindex="0">
                <label class="checkbox">
                    <input type="checkbox" checked class="checkbox--input" id="1"/>
                    <span class="checkbox--icon"></span>
                    First
                </label>
            </li>
            <li role="option" class="dropdown--option" tabindex="0">
                <label class="checkbox">
                    <input type="checkbox" class="checkbox--input" id="2"/>
                    <span class="checkbox--icon"></span>
                    Second
                </label>
            </li>
        </ul>
    </div>
</#macro>
