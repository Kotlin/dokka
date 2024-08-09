<#import "source_set_selector.ftl" as source_set_selector>
<#macro display>
    <nav class="navigation theme-dark" id="navigation-wrapper">
        <@template_cmd name="pathToRoot">
            <a class="library-name--link" href="${pathToRoot}index.html">
                <@template_cmd name="projectName">
                    ${projectName}
                </@template_cmd>
            </a>
        </@template_cmd>
        <button class="navigation-controls--btn navigation-controls--btn_menu ui-kit_mobile-only" id="menu-toggle"
                type="button">Toggle table of content
        </button>
        <div class="navigation-controls--break ui-kit_mobile-only"></div>
        <div class="library-version">
            <#-- This can be handled by the versioning plugin -->
            <@version/>
        </div>
        <div class="navigation-controls">
            <@source_set_selector.display/>
            <#if homepageLink?has_content>
                <a class="navigation-controls--btn navigation-controls--btn_homepage" id="homepage-link"
                   href="${homepageLink}"></a>
            </#if>
            <button class="navigation-controls--btn navigation-controls--btn_theme" id="theme-toggle-button"
                    type="button">Switch theme
            </button>
            <#if sourceSets?has_content>
                <button class="navigation-controls--btn navigation-controls--btn_filter ui-kit_mobile-only"
                        id="platform-tags-toggle" type="button">Toggle source set
                </button>
            </#if>
            <div class="navigation-controls--btn navigation-controls--btn_search" id="searchBar" role="button">Search in
                API
            </div>
        </div>
    </nav>
</#macro>
