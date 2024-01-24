/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gfm.templateProcessing

import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.allModulesPage.MultimoduleLocationProvider
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.jekyll.JekyllPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

public class JekyllTemplateProcessingPlugin : DokkaPlugin() {

    private val allModulesPagePlugin by lazy { plugin<AllModulesPagePlugin>() }

    private val jekyllPlugin by lazy { plugin<JekyllPlugin>() }
    private val gfmTemplatingPlugin by lazy { plugin<GfmTemplateProcessingPlugin>() }

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    public val jekyllLocationProvider: Extension<LocationProviderFactory, *, *> by extending {
        dokkaBase.locationProviderFactory providing MultimoduleLocationProvider::Factory override listOf(
            jekyllPlugin.locationProvider,
            gfmTemplatingPlugin.gfmLocationProvider
        )
    }

    public val jekyllPartialLocationProvider: Extension<LocationProviderFactory, *, *> by extending {
        allModulesPagePlugin.partialLocationProviderFactory providing ::DokkaLocationProviderFactory override listOf(
            allModulesPagePlugin.baseLocationProviderFactory,
            gfmTemplatingPlugin.gfmPartialLocationProvider
        )
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
