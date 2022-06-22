package org.jetbrains.dokka.gfm

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.location.MarkdownLocationProvider
import org.jetbrains.dokka.gfm.renderer.BriefCommentPreprocessor
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.validity.PreGenerationChecker
import org.jetbrains.dokka.validity.PreGenerationCheckerOutput

class GfmPlugin : DokkaPlugin() {

    val gfmPreprocessors by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val renderer by extending {
        CoreExtensions.renderer providing ::CommonmarkRenderer override dokkaBase.htmlRenderer
    }

    val locationProvider by extending {
        dokkaBase.locationProviderFactory providing MarkdownLocationProvider::Factory override dokkaBase.locationProvider
    }

    val rootCreator by extending {
        gfmPreprocessors with RootCreator
    }

    val briefCommentPreprocessor by extending {
        gfmPreprocessors with BriefCommentPreprocessor()
    }

    val packageListCreator by extending {
        (gfmPreprocessors
                providing { PackageListCreator(it, RecognizedLinkFormat.DokkaGFM) }
                order { after(rootCreator) })
    }

    val alphaVersionNotifier by extending {
        CoreExtensions.preGenerationCheck providing { ctx ->
            PreGenerationChecker {
                ctx.logger.warn("The GFM output format is in Alpha version")
                PreGenerationCheckerOutput(true, emptyList())
            }
        }
    }
}
