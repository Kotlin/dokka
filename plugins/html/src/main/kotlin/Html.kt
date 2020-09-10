package org.jetbrains.dokka.html

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.html.renderer.HtmlRenderer
import org.jetbrains.dokka.html.tabs.DefaultTabSortingStrategy
import org.jetbrains.dokka.html.tabs.TabSortingStrategy
import org.jetbrains.dokka.location.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.processing.Processing
import org.jetbrains.dokka.processing.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.transformers.pages.PageTransformer

class Html : DokkaPlugin() {
    val htmlPreprocessors by extensionPoint<PageTransformer>()
    val tabSortingStrategy by extensionPoint<TabSortingStrategy>()

    val defaultTabSortingStrategy by extending {
        tabSortingStrategy with DefaultTabSortingStrategy()
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer
    }

    val rootCreator by extending {
        htmlPreprocessors with RootCreator
    }


    val sourceLinksTransformer by extending {
        htmlPreprocessors providing {
            SourceLinksTransformer(
                it,
                PageContentBuilder(
                    plugin<Processing>().querySingle { commentsToContentTranslator },
                    plugin<Processing>().querySingle { signatureProvider },
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