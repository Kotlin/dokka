package org.jetbrains.dokka.jekyll

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.renderer.BriefCommentPreprocessor
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer

class JekyllPlugin : DokkaPlugin() {

    val jekyllPreprocessors by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    private val gfmPlugin by lazy { plugin<GfmPlugin>() }

    val renderer by extending {
        (CoreExtensions.renderer
                providing { JekyllRenderer(it) }
                override plugin<GfmPlugin>().renderer)
    }

    val rootCreator by extending {
        jekyllPreprocessors with RootCreator
    }

    val briefCommentPreprocessor by extending {
        jekyllPreprocessors with BriefCommentPreprocessor()
    }

    val packageListCreator by extending {
        jekyllPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaJekyll)
        } order { after(rootCreator) }
    }

    val locationProvider by extending {
        dokkaBase.locationProviderFactory providing ::DokkaLocationProviderFactory override listOf(gfmPlugin.locationProvider)
    }
}

class JekyllRenderer(
    context: DokkaContext
) : CommonmarkRenderer(context) {

    override val preprocessors = context.plugin<JekyllPlugin>().query { jekyllPreprocessors }

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String {
        val builder = StringBuilder()
        builder.append("---\n")
        builder.append("title: ${page.name}\n")
        builder.append("---\n")
        content(builder, page)
        return builder.toString()
    }
}
