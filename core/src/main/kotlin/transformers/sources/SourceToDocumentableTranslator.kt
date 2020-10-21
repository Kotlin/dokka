package org.jetbrains.dokka.transformers.sources

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext

interface SourceToDocumentableTranslator {
    suspend fun invoke(sourceSet: DokkaSourceSet, context: DokkaContext): DModule
}

abstract class JavaSourceToDocumentableTranslator: SourceToDocumentableTranslator {
    override suspend fun invoke(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DModule =
        process(sourceSet, context)

    abstract fun process(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DModule
}