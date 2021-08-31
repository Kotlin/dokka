package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.CoreExtensions.postActions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.templates.TemplatingPlugin

class VersioningPlugin : DokkaPlugin() {

    val versioningStorage by extensionPoint<VersioningStorage>()
    val versionsNavigationCreator by extensionPoint<VersionsNavigationCreator>()
    val versionsOrdering by extensionPoint<VersionsOrdering>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }
    private val templatingPlugin by lazy { plugin<TemplatingPlugin>() }

    val defaultVersioningStorage by extending {
        versioningStorage providing ::DefaultVersioningStorage
    }
    val defaultVersioningNavigationCreator by extending {
        versionsNavigationCreator providing ::HtmlVersionsNavigationCreator
    }
    val replaceVersionCommandHandler by extending {
        templatingPlugin.directiveBasedCommandHandlers providing ::ReplaceVersionCommandHandler override templatingPlugin.replaceVersionCommandHandler
    }
    val resolveLinkConsumer by extending {
        dokkaBase.immediateHtmlCommandConsumer providing ::ReplaceVersionCommandConsumer override dokkaBase.replaceVersionConsumer
    }
    val cssStyleInstaller by extending {
        dokkaBase.htmlPreprocessors providing ::MultiModuleStylesInstaller order {
            after(dokkaBase.assetsInstaller)
            before(dokkaBase.customResourceInstaller)
        }
    }
    val notFoundPageInstaller by extending {
        dokkaBase.htmlPreprocessors providing ::NotFoundPageInstaller order {
            after(dokkaBase.assetsInstaller)
            before(dokkaBase.customResourceInstaller)
        } applyIf { !delayTemplateSubstitution }
    }
    val versionsDefaultOrdering by extending {
        versionsOrdering providing { ctx ->
            configuration<VersioningPlugin, VersioningConfiguration>(ctx)?.versionsOrdering?.let {
                ByConfigurationVersionOrdering(ctx)
            } ?: SemVerVersionOrdering()
        }
    }
    val previousDocumentationCopyPostAction by extending {
        postActions providing ::DefaultPreviousDocumentationCopyPostAction applyIf { !delayTemplateSubstitution }
    }
}