<#macro display>
    <div id="storybook">
        <p>Just icon</p>
        <button class="button">
            <div class="ui-kit-icon ui-kit-icon_placeholder"></div>
        </button>
        <p>Just label</p>
        <button class="button">
            Label
        </button>
        <p>Icon and background</p>
        <button class="button button_background">
            <div class="ui-kit-icon ui-kit-icon_placeholder"></div>
        </button>
        <p>Icon and label</p>
        <button class="button">
            <div class="ui-kit-icon ui-kit-icon_placeholder"></div>
        </button>
        <p>Icon, background and label</p>
        <button class="button button_background">
            <div class="ui-kit-icon ui-kit-icon_placeholder"></div>
            Label
        </button>
        <p>dropdown</p>
        <button class="button button_dropdown" onclick="onToggleDropdown(event)">
        </button>
        <p>label with dropdown</p>
        <button class="button button_dropdown" onclick="onToggleDropdown(event)">
            Label
        </button>
    </div>
</#macro>