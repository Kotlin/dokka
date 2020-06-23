package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode

interface DocumentableToPageTranslator {
    operator fun invoke(module: DModule): RootPageNode
}