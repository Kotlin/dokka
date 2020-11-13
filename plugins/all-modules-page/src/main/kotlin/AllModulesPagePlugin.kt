package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.allModulesPage.templates.*
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.transformers.pages.PageTransformer

class AllModulesPagePlugin : DokkaPlugin() {

    val templateProcessor by extensionPoint<TemplateProcessor>()
    val templateProcessingStrategy by extensionPoint<TemplateProcessingStrategy>()
    val partialLocationProviderFactory by extensionPoint<LocationProviderFactory>()
    val allModulesPageCreator by extensionPoint<PageCreator>()
    val allModulesPageTransformer by extensionPoint<PageTransformer>()
    val externalModuleLinkResolver by extensionPoint<ExternalModuleLinkResolver>()

    val substitutor by extensionPoint<Substitutor>()

    val allModulesPageCreators by extending {
        allModulesPageCreator providing ::MultimodulePageCreator
    }

    val multimoduleLocationProvider by extending {
        (plugin<DokkaBase>().locationProviderFactory
                providing MultimoduleLocationProvider::Factory
                override plugin<DokkaBase>().locationProvider)
    }

    val baseLocationProviderFactory by extending {
        partialLocationProviderFactory providing ::DokkaLocationProviderFactory
    }

    val allModulesPageGeneration by extending {
        (CoreExtensions.generation
                providing ::AllModulesPageGeneration
                override plugin<DokkaBase>().singleGeneration)
    }

    val defaultTemplateProcessor by extending {
        templateProcessor providing ::DefaultTemplateProcessor
    }

    val directiveBasedHtmlTemplateProcessingStrategy by extending {
        templateProcessingStrategy providing ::DirectiveBasedHtmlTemplateProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val fallbackProcessingStrategy by extending {
        templateProcessingStrategy providing ::FallbackTemplateProcessingStrategy
    }

    val navigationSearchTemplateStrategy by extending {
        templateProcessingStrategy providing ::NavigationSearchTemplateStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val pagesSearchTemplateStrategy by extending {
        templateProcessingStrategy providing ::PagesSearchTemplateStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val pathToRootSubstitutor by extending {
        substitutor providing ::PathToRootSubstitutor
    }

    val multiModuleLinkResolver by extending {
        externalModuleLinkResolver providing ::DefaultExternalModuleLinkResolver
    }
}