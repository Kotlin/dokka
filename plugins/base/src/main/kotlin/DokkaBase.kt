package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.allModulePage.MultimodulePageCreator
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.*
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.resolvers.external.*
import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.documentables.DefaultDocumentableMerger
import org.jetbrains.dokka.base.transformers.documentables.ModuleAndPackageDocumentationTransformer
import org.jetbrains.dokka.base.transformers.documentables.ReportUndocumentedTransformer
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.FallbackPageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.merger.PageMerger
import org.jetbrains.dokka.base.transformers.pages.merger.PageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.merger.SameMethodNamePageMergerStrategy
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.base.translators.descriptors.DefaultDescriptorToDocumentableTranslator
import org.jetbrains.dokka.base.translators.documentables.DefaultDocumentableToPageTranslator
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.base.translators.psi.DefaultPsiToDocumentableTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DokkaBase : DokkaPlugin() {
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val commentsToContentConverter by extensionPoint<CommentsToContentConverter>()
    val signatureProvider by extensionPoint<SignatureProvider>()
    val locationProviderFactory by extensionPoint<LocationProviderFactory>()
    val externalLocationProviderFactory by extensionPoint<ExternalLocationProviderFactory>()
    val outputWriter by extensionPoint<OutputWriter>()
    val htmlPreprocessors by extensionPoint<PageTransformer>()

    val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator with DefaultDescriptorToDocumentableTranslator
    }

    val psiToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator with DefaultPsiToDocumentableTranslator
    }

    val documentableMerger by extending(isFallback = true) {
        CoreExtensions.documentableMerger with DefaultDocumentableMerger
    }

    val deprecatedDocumentableFilter by extending(isFallback = true) {
        CoreExtensions.preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    val documentableVisbilityFilter by extending(isFallback = true) {
        CoreExtensions.preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    val emptyPackagesFilter by extending(isFallback = true) {
        CoreExtensions.preMergeDocumentableTransformer providing ::EmptyPackagesFilterTransformer order {
            after(deprecatedDocumentableFilter, documentableVisbilityFilter)
        }
    }

    val actualTypealiasAdder by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    val modulesAndPackagesDocumentation by extending(isFallback = true) {
        CoreExtensions.preMergeDocumentableTransformer providing ::ModuleAndPackageDocumentationTransformer
    }

    val kotlinSignatureProvider by extending(isFallback = true) {
        signatureProvider providing { ctx ->
            KotlinSignatureProvider(ctx.single(commentsToContentConverter), ctx.logger)
        }
    }

    val sinceKotlinTransformer by extending {
        CoreExtensions.documentableTransformer providing ::SinceKotlinTransformer
    }

    val inheritorsExtractor by extending {
        CoreExtensions.documentableTransformer with InheritorsExtractorTransformer()
    }


    val undocumentedCodeReporter by extending {
        CoreExtensions.documentableTransformer with ReportUndocumentedTransformer()
    }

    val extensionsExtractor by extending {
        CoreExtensions.documentableTransformer with ExtensionExtractorTransformer()
    }

    val documentableToPageTranslator by extending(isFallback = true) {
        CoreExtensions.documentableToPageTranslator providing { ctx ->
            DefaultDocumentableToPageTranslator(
                ctx.single(commentsToContentConverter),
                ctx.single(signatureProvider),
                ctx.logger
            )
        }
    }

    val docTagToContentConverter by extending(isFallback = true) {
        commentsToContentConverter with DocTagToContentConverter
    }

    val pageMerger by extending {
        CoreExtensions.pageTransformer providing { ctx -> PageMerger(ctx[pageMergerStrategy]) }
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
        locationProviderFactory providing ::DefaultLocationProviderFactory
    }

    val javadocLocationProvider by extending {
        externalLocationProviderFactory with JavadocExternalLocationProviderFactory()
    }

    val dokkaLocationProvider by extending {
        externalLocationProviderFactory with DokkaExternalLocationProviderFactory()
    }

    val fileWriter by extending(isFallback = true) {
        outputWriter providing ::FileWriter
    }

    val rootCreator by extending {
        htmlPreprocessors with RootCreator applyIf { format == "html" }
    }

    val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer order {
            before(pageMerger)
        }
    }

    val sourceLinksTransformer by extending {
        htmlPreprocessors providing {
            SourceLinksTransformer(
                it,
                PageContentBuilder(
                    it.single(commentsToContentConverter),
                    it.single(signatureProvider),
                    it.logger
                )
            )
        } order { after(rootCreator) }
    }

    val navigationPageInstaller by extending {
        htmlPreprocessors with NavigationPageInstaller order { after(rootCreator) } applyIf { format == "html" }
    }

    val searchPageInstaller by extending {
        htmlPreprocessors with SearchPageInstaller order { after(rootCreator) } applyIf { format == "html" }
    }

    val resourceInstaller by extending {
        htmlPreprocessors with ResourceInstaller order { after(rootCreator) } applyIf { format == "html" }
    }

    val styleAndScriptsAppender by extending {
        htmlPreprocessors with StyleAndScriptsAppender order { after(rootCreator) } applyIf { format == "html" }
    }

    val packageListCreator by extending {
        htmlPreprocessors providing {
            PackageListCreator(
                it,
                "html",
                "html"
            )
        } order { after(rootCreator) } applyIf { format == "html" }
    }

    val sourcesetDependencyAppender by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }

    val allModulePageCreators by extending(isFallback = true) {
        CoreExtensions.allModulePageCreator providing {
            MultimodulePageCreator(it)
        }
    }
}