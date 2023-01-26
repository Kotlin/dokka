<#import "source_set_selector.ftl" as source_set_selector>
<#macro display>
<nav class="navigation" id="navigation-wrapper">
    <button class="menu-toggle" id="menu-toggle" type="button">Menu Toggle</button>
    <div class="library-name">
        <@template_cmd name="pathToRoot">
            <a class="library-name--link" href="${pathToRoot}index.html">
                <@template_cmd name="projectName">
                    ${projectName}
                </@template_cmd>
            </a>
        </@template_cmd>
    </div>
    <div class="library-version">
        <#-- This can be handled by the versioning plugin -->
        <@version/>
    </div>
    <div class="navigation-controls">
        <@source_set_selector.display/>
        <button class="navigation-controls--btn navigation-controls--theme" id="theme-toggle-button" type="button"></button>
        <button class="navigation-controls--btn navigation-controls--search" id="searchBar" type="button"></button>
    </div>
</nav>
</#macro>
