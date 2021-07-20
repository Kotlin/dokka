package org.jetbrains.dokka.sitemap

import org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.sitemap.transformers.pages.SitemapPageTransformer
import org.jetbrains.dokka.sitemap.transformers.pages.SitemapTemplateProcessingStrategy
import org.jetbrains.dokka.templates.TemplatingPlugin

data class SitemapConfiguration(
    var baseUrl: String? = defaultBaseUrl,
    var relativeOutputLocation: String? = defaultRelativeOutputLocation
) : ConfigurableBlock {
    companion object {
        val defaultBaseUrl: String? = null
        const val defaultRelativeOutputLocation: String = "sitemap.txt"
    }
}

class SitemapPlugin : DokkaPlugin() {
    private val templatingPlugin by lazy { plugin<TemplatingPlugin>() }

    val sitemapPageTransformer by extending {
        with(plugin<DokkaBase>()) {
            doLastTransformers providing ::SitemapPageTransformer
        }
    }
    val sitemapMultiModulePageTransformer by extending {
        with(plugin<AllModulesPagePlugin>()) {
            allModulesPageTransformer providing ::SitemapPageTransformer applyIf { delayTemplateSubstitution }
        }
    }

    val sourcesetDependencyProcessingStrategy by extending {
        templatingPlugin.templateProcessingStrategy providing ::SitemapTemplateProcessingStrategy order {
            before(templatingPlugin.fallbackProcessingStrategy)
        }
    }
}
