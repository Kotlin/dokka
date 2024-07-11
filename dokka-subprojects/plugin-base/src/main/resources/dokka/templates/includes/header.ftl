<#import "source_set_selector.ftl" as source_set_selector>
<#macro display>
<nav class="navigation" id="navigation-wrapper">
    <div class="navigation-title">
        <@template_cmd name="pathToRoot">
            <a class="library-name--link" href="${pathToRoot}index.html">
                <@template_cmd name="projectName">
                    ${projectName}
                </@template_cmd>
            </a>
        </@template_cmd>
        <div class="library-version">
            <#-- This can be handled by the versioning plugin -->
            <@version/>
        </div>
    </div>
    <div class="navigation-controls">
        <@source_set_selector.display/>
        <#if homepageLink?has_content>
            <a class="navigation-controls--btn navigation-controls--homepage" id="homepage-link" href="${homepageLink}"></a>
        </#if>
        <button class="navigation-controls--btn navigation-controls--theme" id="theme-toggle-button" type="button">switch theme</button>
        <div class="navigation-controls--btn navigation-controls--search" id="searchBar" role="button">search in API</div>
        <button class="menu-toggle navigation-controls--btn" id="menu-toggle" type="button">Toggle table of content</button>
    </div>
</nav>
</#macro>
