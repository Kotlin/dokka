package org.jetbrains.dokka.gfm.allModulesPage

import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.allModulesPage.templates.DefaultTemplateProcessor
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin

class GfmAllModulesPagePlugin : DokkaPlugin() {

    private val gfmPlugin by lazy { plugin<GfmPlugin>() }

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    private val allModulesPagePlugin by lazy { plugin<AllModulesPagePlugin>() }

    val locationProvider by extending {
        (dokkaBase.locationProviderFactory providing GfmMultimoduleLocationProvider::Factory
                override listOf(allModulesPagePlugin.multimoduleLocationProvider, gfmPlugin.locationProvider))
    }

    val defaultTemplateProcessor by extending {
        (allModulesPagePlugin.templateProcessingStrategy
                providing ::GfmTemplateProcessingStrategy
                override allModulesPagePlugin.directiveBasedHtmlTemplateProcessingStrategy
                order { before(allModulesPagePlugin.fallbackProcessingStrategy) })
    }
}