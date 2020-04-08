package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModuleView
import org.jetbrains.dokka.plugability.DokkaContext

interface DocumentableTransformer {
    operator fun invoke(original: DModuleView, context: DokkaContext): DModuleView
}