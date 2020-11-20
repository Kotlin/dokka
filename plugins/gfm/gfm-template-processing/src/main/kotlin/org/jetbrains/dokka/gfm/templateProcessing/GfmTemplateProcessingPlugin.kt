package org.jetbrains.dokka.gfm.templateProcessing

import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.allModulesPage.MultimoduleLocationProvider
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.location.MarkdownLocationProvider
import org.jetbrains.dokka.plugability.DokkaPlugin

class GfmTemplateProcessingPlugin : DokkaPlugin() {

    private val allModulesPagePlugin by lazy { plugin<AllModulesPagePlugin>() }

    private val gfmPlugin by lazy { plugin<GfmPlugin>() }

    private val dokkaBase by lazy { plugin<DokkaBase>()}

    val gfmTemplateProcessingStrategy by extending {
        (allModulesPagePlugin.templateProcessingStrategy
                providing ::GfmTemplateProcessingStrategy
                order { before(allModulesPagePlugin.fallbackProcessingStrategy) })
    }

    val gfmLocationProvider by extending {
        dokkaBase.locationProviderFactory providing MultimoduleLocationProvider::Factory override listOf(gfmPlugin.locationProvider, allModulesPagePlugin.multimoduleLocationProvider)
    }

    val gfmPartialLocationProvider by extending {
        allModulesPagePlugin.partialLocationProviderFactory providing MarkdownLocationProvider::Factory override allModulesPagePlugin.baseLocationProviderFactory
    }
}