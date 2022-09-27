package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode

fun interface DocumentableToPageTranslator {
    operator fun invoke(module: DModule): RootPageNode
}