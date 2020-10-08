package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.allModulesPage.templates.*
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin

class AllModulesPagePlugin : DokkaPlugin() {
    val templateProcessor by extensionPoint<TemplateProcessor>()

    val substitutor by extensionPoint<Substitutor>()

    val allModulePageCreators by extending {
        (CoreExtensions.allModulePageCreator
                providing ::MultimodulePageCreator)
    }

    val multimoduleLocationProvider by extending {
        (plugin<DokkaBase>().locationProviderFactory
                providing MultimoduleLocationProvider::Factory
                override plugin<DokkaBase>().locationProvider
                applyIf { modules.size > 1 })
    }

    val allModulesPageGeneration by extending {
        (CoreExtensions.generation
                providing ::AllModulesPageGeneration
                override CoreExtensions.singleGeneration)
    }

    val defaultTemplateProcessor by extending {
        templateProcessor providing { DefaultTemplateProcessor(it, DirectiveBasedTemplateProcessingStrategy(it)) }
    }

    val pathToRootSubstitutor by extending {
        substitutor providing ::PathToRootSubstitutor
    }
}