/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.CoreExtensions.postActions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.templates.CommandHandler
import org.jetbrains.dokka.templates.TemplatingPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class VersioningPlugin : DokkaPlugin() {

    public val versioningStorage: ExtensionPoint<VersioningStorage> by extensionPoint()
    public val versionsNavigationCreator: ExtensionPoint<VersionsNavigationCreator> by extensionPoint()
    public val versionsOrdering: ExtensionPoint<VersionsOrdering> by extensionPoint()

    private val dokkaBase by lazy { plugin<DokkaBase>() }
    private val templatingPlugin by lazy { plugin<TemplatingPlugin>() }

    public val defaultVersioningStorage: Extension<VersioningStorage, *, *> by extending {
        versioningStorage providing ::DefaultVersioningStorage
    }

    public val defaultVersioningNavigationCreator: Extension<VersionsNavigationCreator, *, *> by extending {
        versionsNavigationCreator providing ::HtmlVersionsNavigationCreator
    }

    public val replaceVersionCommandHandler: Extension<CommandHandler, *, *> by extending {
        templatingPlugin.directiveBasedCommandHandlers providing ::ReplaceVersionCommandHandler override templatingPlugin.replaceVersionCommandHandler
    }

    public val resolveLinkConsumer: Extension<ImmediateHtmlCommandConsumer, *, *> by extending {
        dokkaBase.immediateHtmlCommandConsumer providing ::ReplaceVersionCommandConsumer override dokkaBase.replaceVersionConsumer
    }

    public val cssStyleInstaller: Extension<PageTransformer, *, *> by extending {
        dokkaBase.htmlPreprocessors providing ::MultiModuleStylesInstaller order {
            after(dokkaBase.assetsInstaller)
            before(dokkaBase.customResourceInstaller)
        }
    }

    public val notFoundPageInstaller: Extension<PageTransformer, *, *> by extending {
        dokkaBase.htmlPreprocessors providing ::NotFoundPageInstaller order {
            after(dokkaBase.assetsInstaller)
            before(dokkaBase.customResourceInstaller)
        } applyIf { !delayTemplateSubstitution }
    }

    public val versionsDefaultOrdering: Extension<VersionsOrdering, *, *> by extending {
        versionsOrdering providing { ctx ->
            configuration<VersioningPlugin, VersioningConfiguration>(ctx)?.versionsOrdering?.let {
                ByConfigurationVersionOrdering(ctx)
            } ?: SemVerVersionOrdering()
        }
    }

    public val previousDocumentationCopyPostAction: Extension<PostAction, *, *> by extending {
        postActions providing ::DefaultPreviousDocumentationCopyPostAction applyIf { !delayTemplateSubstitution }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
