/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.templates.CommandHandler
import org.jetbrains.dokka.templates.TemplatingPlugin
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class AllModulesPagePlugin : DokkaPlugin() {

    public val partialLocationProviderFactory: ExtensionPoint<LocationProviderFactory> by extensionPoint()
    public val allModulesPageCreator: ExtensionPoint<PageCreator<AllModulesPageGeneration.DefaultAllModulesContext>> by extensionPoint()
    public val allModulesPageTransformer: ExtensionPoint<PageTransformer> by extensionPoint()
    public val externalModuleLinkResolver: ExtensionPoint<ExternalModuleLinkResolver> by extensionPoint()

    public val allModulesPageCreators: Extension<PageCreator<AllModulesPageGeneration.DefaultAllModulesContext>, *, *> by extending {
        allModulesPageCreator providing ::MultimodulePageCreator
    }

    private val dokkaBase: DokkaBase by lazy { plugin<DokkaBase>() }

    public val multimoduleLocationProvider: Extension<LocationProviderFactory, *, *> by extending {
        (dokkaBase.locationProviderFactory
                providing MultimoduleLocationProvider::Factory
                override plugin<DokkaBase>().locationProvider)
    }

    public val baseLocationProviderFactory: Extension<LocationProviderFactory, *, *> by extending {
        partialLocationProviderFactory providing ::DokkaLocationProviderFactory
    }

    public val allModulesPageGeneration: Extension<Generation, *, *> by extending {
        (CoreExtensions.generation
                providing ::AllModulesPageGeneration
                override dokkaBase.singleGeneration)
    }

    public val resolveLinkCommandHandler: Extension<CommandHandler, *, *> by extending {
        plugin<TemplatingPlugin>().directiveBasedCommandHandlers providing ::ResolveLinkCommandHandler
    }

    public val multiModuleLinkResolver: Extension<ExternalModuleLinkResolver, *, *> by extending {
        externalModuleLinkResolver providing ::DefaultExternalModuleLinkResolver
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
