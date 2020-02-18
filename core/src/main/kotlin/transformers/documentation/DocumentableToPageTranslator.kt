package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode

interface DocumentableToPageTranslator {
    operator fun invoke(module: Module): ModulePageNode
}