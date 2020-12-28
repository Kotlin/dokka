package org.jetbrains.dokka.webhelp.renderers.preprocessors

import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer

object ConfigurationAppender : PageTransformer {
    private val buildScript = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE root
                    SYSTEM "https://resources.jetbrains.com/stardust/build-script.dtd">
            <root>
                <builds>
                    <product id="s" master="project.ihp" family="idea">
                        <artifact type="web2" name="webHelpS2.zip" platform="primary" local-update="true"/>
                    </product>
                </builds>
            </root>""".trimIndent()

    private val buildProfiles =
        """
            <?xml version="1.0" encoding="UTF-8"?>

            <buildprofiles>
                <variables>
                    <list-bullets>blue_romb_blt.png</list-bullets>
                    <list-links>seealso_blt.gif</list-links>
                    <toc-image-doc>topic.gif</toc-image-doc>
                    <toc-image-folder>folder.gif</toc-image-folder>
                    <marker-note>note.gif</marker-note>
                    <marker-warning>notice_purple.gif</marker-warning>
                    <marker-tip>tip.gif</marker-tip>
                    <marker-extlink>mark.gif</marker-extlink>
                    <printable-marker>iconpdf.png</printable-marker>
                    <marker-arrowup>moveUp.gif</marker-arrowup>
                    <hr-delimiter>hr.gif</hr-delimiter>
                    <header-background>bg_idea_big.jpg</header-background>
                    <toc-image-root>IDEA.gif</toc-image-root>
                    <images-home>images</images-home>
                    <printable-marker>printable.png</printable-marker>
                    <stylesheets-home>css</stylesheets-home>
                    <pdf-bullet>blt_pdf.png</pdf-bullet>
                    <web-root>http://www.jetbrains.com/idea/</web-root>
                    <web-community-path>Developer Community:http://www.jetbrains.net/devnet/community/idea/kb</web-community-path>
                    <download-page/>
                    <help-app-version>2</help-app-version>
                    <config-disqus-id>jetbrains</config-disqus-id>
                    <config-disqus-show>true</config-disqus-show>
                    <config-feedback-enabled>true</config-feedback-enabled>
                    <config-feedback-widget>true</config-feedback-widget>
                    <config-feedback-support>null</config-feedback-support>
                    <config-feedback-url>https://support.jetbrains.com</config-feedback-url>
                    <config-webmaster>webmaster@jetbrains.com</config-webmaster>
                    <config-sideblocks>true</config-sideblocks>
                    <config-search-scopes-provider>https://www.jetbrains.com/search/json/</config-search-scopes-provider>
                </variables>
                <layout name="New Web help" mode="web2">
                    <style>s.css</style>
                    <views toc="true" search="true" index="true" fav="true"/>
                    <xsl>topic.web.new.xsl</xsl>
                </layout>
                <build-profile product="s"/>
            </buildprofiles>
        """.trimIndent()

    override fun invoke(input: RootPageNode): RootPageNode =
        listOf(
            "cfg/build-script.xml" to buildScript,
            "cfg/buildprofiles.xml" to buildProfiles
        ).map { (path, content) ->
            RendererSpecificResourcePage(
                name = path,
                children = emptyList(),
                strategy = RenderingStrategy.Write(content)
            )
        }.let { input.modified(children = input.children + it) }
}