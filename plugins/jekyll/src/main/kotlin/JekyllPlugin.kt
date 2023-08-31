/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.jekyll

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.renderer.BriefCommentPreprocessor
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class JekyllPlugin : DokkaPlugin() {

    public val jekyllPreprocessors: ExtensionPoint<PageTransformer> by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    private val gfmPlugin by lazy { plugin<GfmPlugin>() }

    public val renderer: Extension<Renderer, *, *> by extending {
        (CoreExtensions.renderer
                providing { JekyllRenderer(it) }
                override plugin<GfmPlugin>().renderer)
    }

    public val rootCreator: Extension<PageTransformer, *, *> by extending {
        jekyllPreprocessors with RootCreator
    }

    public val briefCommentPreprocessor: Extension<PageTransformer, *, *> by extending {
        jekyllPreprocessors with BriefCommentPreprocessor()
    }

    public val packageListCreator: Extension<PageTransformer, *, *> by extending {
        jekyllPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaJekyll)
        } order { after(rootCreator) }
    }

    public val locationProvider: Extension<LocationProviderFactory, *, *> by extending {
        dokkaBase.locationProviderFactory providing ::DokkaLocationProviderFactory override listOf(gfmPlugin.locationProvider)
    }

    internal val alphaVersionNotifier by extending {
        CoreExtensions.postActions providing { ctx ->
            PostAction {
                ctx.logger.info(
                    "The Jekyll output format is still in Alpha so you may find bugs and experience migration " +
                            "issues when using it. You use it at your own risk."
                )
            }
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

public class JekyllRenderer(
    context: DokkaContext
) : CommonmarkRenderer(context) {

    override val preprocessors: List<PageTransformer> = context.plugin<JekyllPlugin>().query { jekyllPreprocessors }

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String {
        val builder = StringBuilder()
        builder.append("---\n")
        builder.append("title: ${page.name}\n")
        builder.append("---\n")
        content(builder, page)
        return builder.toString()
    }
}
