package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.templates.TemplatingPlugin

class VersioningPlugin : DokkaPlugin() {

    val versioningHandler by extensionPoint<VersioningHandler>()
    val versionsNavigationCreator by extensionPoint<VersionsNavigationCreator>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }
    private val templatingPlugin by lazy { plugin<TemplatingPlugin>() }

    val defaultVersioningHandler by extending {
        versioningHandler providing ::DefaultVersioningHandler
    }
    val defaultVersioningNavigationCreator by extending {
        versionsNavigationCreator providing ::HtmlVersionsNavigationCreator
    }
    val replaceVersionCommandHandler by extending {
        templatingPlugin.directiveBasedCommandHandlers providing ::ReplaceVersionCommandHandler
    }
    val resolveLinkConsumer by extending {
        dokkaBase.immediateHtmlCommandConsumer providing ::ReplaceVersionCommandConsumer
    }
    val cssStyleInstaller by extending {
        dokkaBase.htmlPreprocessors with MultiModuleStylesInstaller order { after(dokkaBase.assetsInstaller) }
    }
}