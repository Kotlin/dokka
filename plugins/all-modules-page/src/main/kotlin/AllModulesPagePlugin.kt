package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.templates.TemplatingPlugin
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.transformers.pages.PageTransformer

class AllModulesPagePlugin : DokkaPlugin() {

    val partialLocationProviderFactory by extensionPoint<LocationProviderFactory>()
    val allModulesPageCreator by extensionPoint<PageCreator>()
    val allModulesPageTransformer by extensionPoint<PageTransformer>()
    val externalModuleLinkResolver by extensionPoint<ExternalModuleLinkResolver>()

    val allModulesPageCreators by extending {
        allModulesPageCreator providing ::MultimodulePageCreator
    }

    val dokkaBase by lazy { plugin<DokkaBase>() }

    val multimoduleLocationProvider by extending {
        (dokkaBase.locationProviderFactory
                providing MultimoduleLocationProvider::Factory
                override plugin<DokkaBase>().locationProvider)
    }

    val baseLocationProviderFactory by extending {
        partialLocationProviderFactory providing ::DokkaLocationProviderFactory
    }

    val allModulesPageGeneration by extending {
        (CoreExtensions.generation
                providing ::AllModulesPageGeneration
                override dokkaBase.singleGeneration)
    }

    val resolveLinkCommandHandler by extending {
        plugin<TemplatingPlugin>().directiveBasedCommandHandlers providing ::ResolveLinkCommandHandler
    }

    val multiModuleLinkResolver by extending {
        externalModuleLinkResolver providing ::DefaultExternalModuleLinkResolver
    }
}