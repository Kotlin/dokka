package org.jetbrains.dokka.transformers.sources

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext

interface SourceToDocumentableTranslator {
    fun invoke(platformData: PlatformData, context: DokkaContext): DModule
}