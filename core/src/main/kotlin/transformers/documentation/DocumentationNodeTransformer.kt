package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.Model.Module
import org.jetbrains.dokka.plugability.DokkaContext

interface DocumentationNodeTransformer {
    operator fun invoke(original: Module, context: DokkaContext): Module
}