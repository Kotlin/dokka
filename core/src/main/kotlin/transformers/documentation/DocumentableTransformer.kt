package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.plugability.DokkaContext

interface DocumentableTransformer {
    operator fun invoke(original: Module, context: DokkaContext): Module
}