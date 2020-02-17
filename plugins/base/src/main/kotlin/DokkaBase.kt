package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.renderers.FileWriter
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.base.resolvers.DefaultLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.LocationProviderFactory
import org.jetbrains.dokka.base.transformers.descriptors.DefaultDescriptorToDocumentationTranslator
import org.jetbrains.dokka.base.transformers.documentables.DefaultDocumentableMerger
import org.jetbrains.dokka.base.transformers.documentables.DefaultDocumentablesToPageTranslator
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.FallbackPageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.merger.PageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.merger.PageNodeMerger
import org.jetbrains.dokka.base.transformers.pages.merger.SameMethodNamePageMergerStrategy
import org.jetbrains.dokka.base.transformers.psi.DefaultPsiToDocumentationTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin

class DokkaBase : DokkaPlugin() {
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val commentsToContentConverter by extensionPoint<CommentsToContentConverter>()
    val locationproviderFactory by extensionPoint<LocationProviderFactory>()
    val outputWriter by extensionPoint<OutputWriter>()

    val descriptorToDocumentationTranslator by extending(isFallback = true) {
        CoreExtensions.descriptorToDocumentationTranslator providing ::DefaultDescriptorToDocumentationTranslator
    }

    val psiToDocumentationTranslator by extending(isFallback = true) {
        CoreExtensions.psiToDocumentationTranslator with DefaultPsiToDocumentationTranslator
    }

    val documentableMerger by extending(isFallback = true) {
        CoreExtensions.documentableMerger with DefaultDocumentableMerger
    }

    val documentablesToPageTranslator by extending(isFallback = true) {
        CoreExtensions.documentablesToPageTranslator providing { ctx ->
            DefaultDocumentablesToPageTranslator(ctx.single(commentsToContentConverter), ctx.logger)
        }
    }

    val docTagToContentConverter by extending(isFallback = true) {
        commentsToContentConverter with DocTagToContentConverter
    }

    val pageMerger by extending {
        CoreExtensions.pageTransformer providing { ctx -> PageNodeMerger(ctx[pageMergerStrategy]) }
    }

    val fallbackMerger by extending {
        pageMergerStrategy providing { ctx -> FallbackPageMergerStrategy(ctx.logger) }
    }

    val sameMethodNameMerger by extending {
        pageMergerStrategy with SameMethodNamePageMergerStrategy order {
            before(fallbackMerger)
        }
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer applyIf { format == "html" }
    }

    val locationProvider by extending(isFallback = true) {
        locationproviderFactory providing ::DefaultLocationProviderFactory
    }

    val fileWriter by extending(isFallback = true) {
        outputWriter providing ::FileWriter
    }
}