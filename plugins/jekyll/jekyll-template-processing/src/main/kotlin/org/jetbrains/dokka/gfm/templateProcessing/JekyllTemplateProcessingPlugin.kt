package org.jetbrains.dokka.gfm.templateProcessing

import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.allModulesPage.MultimoduleLocationProvider
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.jekyll.JekyllPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin

class JekyllTemplateProcessingPlugin : DokkaPlugin() {

    private val allModulesPagePlugin by lazy { plugin<AllModulesPagePlugin>() }

    private val jekyllPlugin by lazy { plugin<JekyllPlugin>() }
    private val gfmTemplatingPlugin by lazy { plugin<GfmTemplateProcessingPlugin>() }

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val jekyllLocationProvider by extending {
        dokkaBase.locationProviderFactory providing MultimoduleLocationProvider::Factory override listOf(
            jekyllPlugin.locationProvider,
            gfmTemplatingPlugin.gfmLocationProvider
        )
    }

    val jekyllPartialLocationProvider by extending {
        allModulesPagePlugin.partialLocationProviderFactory providing ::DokkaLocationProviderFactory override listOf(
            allModulesPagePlugin.baseLocationProviderFactory,
            gfmTemplatingPlugin.gfmPartialLocationProvider
        )
    }
}
