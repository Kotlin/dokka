package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext

interface DocumentablesToPageTranslator {
    operator fun invoke(module: Module, context: DokkaContext): ModulePageNode
}