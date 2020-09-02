package org.jetbrains.dokka.transformers.sources

import org.jetbrains.dokka.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaModuleContext

interface SourceToDocumentableTranslator {
    fun invoke(sourceSet: DokkaSourceSet, context: DokkaModuleContext): DModule
}
