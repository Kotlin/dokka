package org.jetbrains.dokka.webhelp.renderers.preprocessors

import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer

object ProjectDefinitionAppender : PageTransformer {
    private val content = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE ihp SYSTEM "https://resources.jetbrains.com/stardust/ihp.dtd">
        
        <ihp version="2.0">
            <categories src="c.list"/>
            <topics dir="topics"/>
            <images dir="images" version="1.0"/>
            <vars src="v.list"/>
            <product src="t.tree" version="1.0"/>
        </ihp>
    """.trimIndent()

    override fun invoke(input: RootPageNode): RootPageNode {
        val page = RendererSpecificResourcePage(
            name = "project.ihp",
            children = emptyList(),
            strategy = RenderingStrategy.Write(content)
        )

        return input.modified(children = input.children + page)
    }

}