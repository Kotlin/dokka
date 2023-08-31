/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import org.jetbrains.dokka.allModulesPage.templates.PackageListProcessingStrategy
import org.jetbrains.dokka.allModulesPage.templates.PagesSearchTemplateStrategy
import org.jetbrains.dokka.plugability.*
import templates.ProjectNameSubstitutor
import templates.ReplaceVersionCommandHandler
import templates.SourcesetDependencyProcessingStrategy

@Suppress("unused")
public class TemplatingPlugin : DokkaPlugin() {

    public val submoduleTemplateProcessor: ExtensionPoint<SubmoduleTemplateProcessor> by extensionPoint()
    public val multimoduleTemplateProcessor: ExtensionPoint<MultiModuleTemplateProcessor> by extensionPoint()
    public val templateProcessingStrategy: ExtensionPoint<TemplateProcessingStrategy> by extensionPoint()
    public val directiveBasedCommandHandlers: ExtensionPoint<CommandHandler> by extensionPoint()
    public val substitutor: ExtensionPoint<Substitutor> by extensionPoint()

    public val defaultSubmoduleTemplateProcessor: Extension<SubmoduleTemplateProcessor, *, *> by extending {
        submoduleTemplateProcessor providing ::DefaultSubmoduleTemplateProcessor
    }

    public val defaultMultiModuleTemplateProcessor: Extension<MultiModuleTemplateProcessor, *, *> by extending {
        multimoduleTemplateProcessor providing ::DefaultMultiModuleTemplateProcessor
    }

    public val directiveBasedHtmlTemplateProcessingStrategy: Extension<TemplateProcessingStrategy, *, *> by extending {
        templateProcessingStrategy providing ::DirectiveBasedHtmlTemplateProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    public val sourcesetDependencyProcessingStrategy: Extension<TemplateProcessingStrategy, *, *> by extending {
        templateProcessingStrategy providing ::SourcesetDependencyProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    public val pagesSearchTemplateStrategy: Extension<TemplateProcessingStrategy, *, *> by extending {
        templateProcessingStrategy providing ::PagesSearchTemplateStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    public val packageListProcessingStrategy: Extension<TemplateProcessingStrategy, *, *> by extending {
        templateProcessingStrategy providing ::PackageListProcessingStrategy order {
            before(fallbackProcessingStrategy)
        }
    }

    public val fallbackProcessingStrategy: Extension<TemplateProcessingStrategy, *, *> by extending {
        templateProcessingStrategy with FallbackTemplateProcessingStrategy()
    }

    public val pathToRootSubstitutor: Extension<Substitutor, *, *> by extending {
        substitutor providing ::PathToRootSubstitutor
    }

    public val projectNameSubstitutor: Extension<Substitutor, *, *> by extending {
        substitutor providing ::ProjectNameSubstitutor
    }

    public val addToNavigationCommandHandler: Extension<CommandHandler, *, *> by extending {
        directiveBasedCommandHandlers providing ::AddToNavigationCommandHandler
    }
    public val substitutionCommandHandler: Extension<CommandHandler, *, *> by extending {
        directiveBasedCommandHandlers providing ::SubstitutionCommandHandler
    }
    public val replaceVersionCommandHandler: Extension<CommandHandler, *, *> by extending {
        directiveBasedCommandHandlers providing ::ReplaceVersionCommandHandler
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
