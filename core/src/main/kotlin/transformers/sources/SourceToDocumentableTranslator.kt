package org.jetbrains.dokka.transformers.sources

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.plugability.DokkaContext

interface SourceToDocumentableTranslator {
    fun invoke(sourceSet: SourceSetData, context: DokkaContext): DModule
}