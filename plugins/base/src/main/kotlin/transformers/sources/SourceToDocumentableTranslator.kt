package org.jetbrains.dokka.base.transformers.sources

import org.jetbrains.dokka.base.plugability.DokkaContext
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetData

interface SourceToDocumentableTranslator {
    fun invoke(sourceSet: SourceSetData, context: DokkaContext): DModule
}