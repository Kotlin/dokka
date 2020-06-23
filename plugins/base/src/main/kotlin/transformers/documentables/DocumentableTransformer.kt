package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.base.plugability.DokkaContext
import org.jetbrains.dokka.model.DModule

interface DocumentableTransformer {
    operator fun invoke(original: DModule, context: DokkaContext): DModule
}