package org.jetbrains.dokka.jekyll

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.lang.StringBuilder


class JekyllPlugin : DokkaPlugin() {

    val jekyllPreprocessors by extensionPoint<PageTransformer>()

    val renderer by extending {
        (CoreExtensions.renderer
                providing { JekyllRenderer(it) }
                applyIf { format == "jekyll" }
                override plugin<DokkaBase>().htmlRenderer)
    }

    val rootCreator by extending {
        jekyllPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        jekyllPreprocessors providing {
            PackageListCreator(
                it,
                "jekyll",
                "md"
            )
        } order { after(rootCreator) }
    }
}

class JekyllRenderer(
    context: DokkaContext
) : CommonmarkRenderer(context) {

    override val preprocessors = context.plugin<JekyllPlugin>().query { jekyllPreprocessors }

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String {
        val builder = StringBuilder()
        builder.append("---\n")
        builder.append("title: ${page.name} -\n")
        builder.append("---\n")
        content(builder, page)
        return builder.toString()
    }
}