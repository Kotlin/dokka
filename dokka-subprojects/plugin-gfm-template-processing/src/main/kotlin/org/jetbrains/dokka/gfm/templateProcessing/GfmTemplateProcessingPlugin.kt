/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gfm.templateProcessing

import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.allModulesPage.MultimoduleLocationProvider
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.location.MarkdownLocationProvider
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin

public class GfmTemplateProcessingPlugin : DokkaPlugin() {

    private val allModulesPagePlugin by lazy { plugin<AllModulesPagePlugin>() }
    private val templateProcessingPlugin by lazy { plugin<TemplatingPlugin>() }
    private val gfmPlugin by lazy { plugin<GfmPlugin>() }
    private val dokkaBase by lazy { plugin<DokkaBase>()}

    public val gfmTemplateProcessingStrategy: Extension<TemplateProcessingStrategy, *, *> by extending {
        (templateProcessingPlugin.templateProcessingStrategy
                providing ::GfmTemplateProcessingStrategy
                order { before(templateProcessingPlugin.fallbackProcessingStrategy) })
    }

    public val gfmLocationProvider: Extension<LocationProviderFactory, *, *> by extending {
        dokkaBase.locationProviderFactory providing MultimoduleLocationProvider::Factory override listOf(gfmPlugin.locationProvider, allModulesPagePlugin.multimoduleLocationProvider)
    }

    public val gfmPartialLocationProvider: Extension<LocationProviderFactory, *, *> by extending {
        allModulesPagePlugin.partialLocationProviderFactory providing MarkdownLocationProvider::Factory override allModulesPagePlugin.baseLocationProviderFactory
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
