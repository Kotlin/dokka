package org.jetbrains.dokka.templates

import org.jetbrains.dokka.allModulesPage.templates.NavigationSearchTemplateStrategy
import org.jetbrains.dokka.allModulesPage.templates.PagesSearchTemplateStrategy
import org.jetbrains.dokka.plugability.DokkaPlugin

class TemplatingPlugin : DokkaPlugin() {

    val templateProcessor by extensionPoint<TemplateProcessor>()
    val templateProcessingStrategy by extensionPoint<TemplateProcessingStrategy>()
    val directiveBasedCommandHandlers by extensionPoint<CommandHandler>()

    val substitutor by extensionPoint<Substitutor>()

    val defaultTemplateProcessor by extending {
        templateProcessor providing ::DefaultTemplateProcessor
    }

    val directiveBasedHtmlTemplateProcessingStrategy by extending {
        templateProcessingStrategy providing ::DirectiveBasedHtmlTemplateProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
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

    val fallbackProcessingStrategy by extending {
        templateProcessingStrategy providing ::FallbackTemplateProcessingStrategy
    }

    val pathToRootSubstitutor by extending {
        substitutor providing ::PathToRootSubstitutor
    }

    val addToNavigationCommandHandler by extending {
        directiveBasedCommandHandlers with AddToNavigationCommandHandler()
    }
    val substitutionCommandHandler by extending {
        directiveBasedCommandHandlers providing ::SubstitutionCommandHandler
    }
}