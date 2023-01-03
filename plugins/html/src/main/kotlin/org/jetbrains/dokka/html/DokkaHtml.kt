@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.jetbrains.dokka.html

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.TabSortingStrategy
import org.jetbrains.dokka.html.renderers.*
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.html.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.html.renderers.command.consumers.*
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DokkaHtml : DokkaPlugin() {
    val htmlPreprocessors by extensionPoint<PageTransformer>()
    val tabSortingStrategy by extensionPoint<TabSortingStrategy>()
    val immediateHtmlCommandConsumer by extensionPoint<ImmediateHtmlCommandConsumer>()

    val defaultTabSortingStrategy by extending {
        tabSortingStrategy with DefaultTabSortingStrategy()
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer
    }

    val rootCreator by extending {
        htmlPreprocessors with RootCreator applyIf { !delayTemplateSubstitution }
    }

    val sourceLinksTransformer by extending {
        htmlPreprocessors providing ::SourceLinksTransformer order { after(rootCreator) }
    }

    val navigationPageInstaller by extending {
        htmlPreprocessors providing ::NavigationPageInstaller order { after(rootCreator) }
    }

    val scriptsInstaller by extending {
        htmlPreprocessors providing ::ScriptsInstaller order { after(rootCreator) }
    }

    val stylesInstaller by extending {
        htmlPreprocessors providing ::StylesInstaller order { after(rootCreator) }
    }

    val assetsInstaller by extending {
        htmlPreprocessors with AssetsInstaller order { after(rootCreator) } applyIf { !delayTemplateSubstitution }
    }

    val customResourceInstaller by extending {
        htmlPreprocessors providing { ctx -> CustomResourceInstaller(ctx) } order {
            after(stylesInstaller)
            after(scriptsInstaller)
            after(assetsInstaller)
        }
    }

    val packageListCreator by extending {
        htmlPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaHtml)
        } order { after(rootCreator) }
    }

    val sourcesetDependencyAppender by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }

    val resolveLinkConsumer by extending {
        immediateHtmlCommandConsumer with ResolveLinkConsumer
    }
    val replaceVersionConsumer by extending {
        immediateHtmlCommandConsumer providing ::ReplaceVersionsConsumer
    }
    val pathToRootConsumer by extending {
        immediateHtmlCommandConsumer with PathToRootConsumer
    }
    val baseSearchbarDataInstaller by extending {
        htmlPreprocessors providing ::SearchbarDataInstaller order { after(sourceLinksTransformer) }
    }
}
