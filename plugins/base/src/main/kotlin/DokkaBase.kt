@file:Suppress("unused")

package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.*
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.merger.*
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.base.translators.documentables.DefaultDocumentableToPageTranslator
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DokkaBase : DokkaPlugin() {
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val outputWriter by extensionPoint<OutputWriter>()
    val htmlPreprocessors by extensionPoint<PageTransformer>()
    val tabSortingStrategy by extensionPoint<TabSortingStrategy>()

    val documentableMerger by extending {
        CoreExtensions.documentableMerger with DefaultDocumentableMerger
    }

    val deprecatedDocumentableFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    val suppressedDocumentableFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::SuppressedDocumentableFilterTransformer
    }

    val documentableVisbilityFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    val emptyPackagesFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::EmptyPackagesFilterTransformer order {
            after(deprecatedDocumentableFilter, suppressedDocumentableFilter, documentableVisbilityFilter)
        }
    }

    val actualTypealiasAdder by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    val modulesAndPackagesDocumentation by extending {
        CoreExtensions.preMergeDocumentableTransformer providing { ctx ->
            ModuleAndPackageDocumentationTransformer(ctx, ctx.single(kotlinAnalysis))
        }
    }

    val sinceKotlinTransformer by extending {
        CoreExtensions.documentableTransformer providing ::SinceKotlinTransformer
    }


    val undocumentedCodeReporter by extending {
        CoreExtensions.documentableTransformer with ReportUndocumentedTransformer()
    }


    val pageMerger by extending {
        CoreExtensions.pageTransformer providing { ctx -> PageMerger(ctx[pageMergerStrategy]) }
    }

    val sourceSetMerger by extending {
        CoreExtensions.pageTransformer providing  ::SourceSetMergingPageTransformer
    }

    val fallbackMerger by extending {
        pageMergerStrategy providing { ctx -> FallbackPageMergerStrategy(ctx.logger) }
    }

    val sameMethodNameMerger by extending {
        pageMergerStrategy providing { ctx -> SameMethodNamePageMergerStrategy(ctx.logger) } order {
            before(fallbackMerger)
        }
    }

    val defaultTabSortingStrategy by extending {
        tabSortingStrategy with DefaultTabSortingStrategy()
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer
    }

    val fileWriter by extending {
        outputWriter providing ::FileWriter
    }

    val rootCreator by extending {
        htmlPreprocessors with RootCreator
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
                    it.single(commentsToContentTranslator),
                    it.single(signatureProvider),
                    it.logger
                )
            )
        } order { after(rootCreator) }
    }

    val navigationPageInstaller by extending {
        htmlPreprocessors with NavigationPageInstaller order { after(rootCreator) }
    }

    val searchPageInstaller by extending {
        htmlPreprocessors with SearchPageInstaller order { after(rootCreator) }
    }

    val resourceInstaller by extending {
        htmlPreprocessors with ResourceInstaller order { after(rootCreator) }
    }

    val styleAndScriptsAppender by extending {
        htmlPreprocessors with StyleAndScriptsAppender order { after(rootCreator) }
    }

    val packageListCreator by extending {
        htmlPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaHtml)
        } order { after(rootCreator) }
    }

    val sourcesetDependencyAppender by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }
}
