@file:Suppress("unused")

package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.merger.*
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DokkaBase : DokkaPlugin() {
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()


    val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer order {
            before(pageMerger)
        }
    }

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
}
