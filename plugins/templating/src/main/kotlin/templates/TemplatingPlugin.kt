package org.jetbrains.dokka.templates

import org.jetbrains.dokka.allModulesPage.templates.PackageListProcessingStrategy
import org.jetbrains.dokka.allModulesPage.templates.PagesSearchTemplateStrategy
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import templates.ProjectNameSubstitutor
import templates.ReplaceVersionCommandHandler
import templates.SourcesetDependencyProcessingStrategy

@Suppress("unused")
class TemplatingPlugin : DokkaPlugin() {

    val submoduleTemplateProcessor by extensionPoint<SubmoduleTemplateProcessor>()
    val multimoduleTemplateProcessor by extensionPoint<MultiModuleTemplateProcessor>()
    val templateProcessingStrategy by extensionPoint<TemplateProcessingStrategy>()
    val directiveBasedCommandHandlers by extensionPoint<CommandHandler>()

    val substitutor by extensionPoint<Substitutor>()

    val defaultSubmoduleTemplateProcessor by extending {
        submoduleTemplateProcessor providing ::DefaultSubmoduleTemplateProcessor
    }

    val defaultMultiModuleTemplateProcessor by extending {
        multimoduleTemplateProcessor providing ::DefaultMultiModuleTemplateProcessor
    }

    val directiveBasedHtmlTemplateProcessingStrategy by extending {
        templateProcessingStrategy providing ::DirectiveBasedHtmlTemplateProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val sourcesetDependencyProcessingStrategy by extending {
        templateProcessingStrategy providing ::SourcesetDependencyProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val pagesSearchTemplateStrategy by extending {
        templateProcessingStrategy providing ::PagesSearchTemplateStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val packageListProcessingStrategy by extending {
        templateProcessingStrategy providing ::PackageListProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    val fallbackProcessingStrategy by extending {
        templateProcessingStrategy with FallbackTemplateProcessingStrategy()
    }

    val pathToRootSubstitutor by extending {
        substitutor providing ::PathToRootSubstitutor
    }

    val projectNameSubstitutor by extending {
        substitutor providing ::ProjectNameSubstitutor
    }

    val addToNavigationCommandHandler by extending {
        directiveBasedCommandHandlers providing ::AddToNavigationCommandHandler
    }
    val substitutionCommandHandler by extending {
        directiveBasedCommandHandlers providing ::SubstitutionCommandHandler
    }
    val replaceVersionCommandHandler by extending {
        directiveBasedCommandHandlers providing ::ReplaceVersionCommandHandler
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
