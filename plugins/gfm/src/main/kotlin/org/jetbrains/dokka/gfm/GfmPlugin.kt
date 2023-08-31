/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gfm

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.location.MarkdownLocationProvider
import org.jetbrains.dokka.gfm.renderer.BriefCommentPreprocessor
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class GfmPlugin : DokkaPlugin() {

    public val gfmPreprocessors: ExtensionPoint<PageTransformer> by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    public val renderer: Extension<Renderer, *, *> by extending {
        CoreExtensions.renderer providing ::CommonmarkRenderer override dokkaBase.htmlRenderer
    }

    public val locationProvider: Extension<LocationProviderFactory, *, *> by extending {
        dokkaBase.locationProviderFactory providing MarkdownLocationProvider::Factory override dokkaBase.locationProvider
    }

    public val rootCreator: Extension<PageTransformer, *, *> by extending {
        gfmPreprocessors with RootCreator
    }

    public val briefCommentPreprocessor: Extension<PageTransformer, *, *> by extending {
        gfmPreprocessors with BriefCommentPreprocessor()
    }

    public val packageListCreator: Extension<PageTransformer, *, *> by extending {
        (gfmPreprocessors
                providing { PackageListCreator(it, RecognizedLinkFormat.DokkaGFM) }
                order { after(rootCreator) })
    }

    internal val alphaVersionNotifier by extending {
        CoreExtensions.postActions providing { ctx ->
            PostAction {
                ctx.logger.info(
                    "The GFM output format is still in Alpha so you may find bugs and experience migration " +
                            "issues when using it. You use it at your own risk."
                )
            }
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
