package org.jetbrains.dokka.jekyll

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import java.lang.StringBuilder


class JekyllPlugin : DokkaPlugin() {

    val renderer by extending {
        CoreExtensions.renderer providing { JekyllRenderer(it) } applyIf { format == "jekyll" }
    }
}

class JekyllRenderer(
    context: DokkaContext
) : CommonmarkRenderer(context) {

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String {
        val builder = StringBuilder()
        builder.append("---\n")
        builder.append("title: ${page.name} -\n")
        builder.append("---\n")
        content(builder, page)
        return builder.toString()
    }
}