package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaModuleContext

interface DocumentableTransformer {
    operator fun invoke(original: DModule, context: DokkaModuleContext): DModule
}
