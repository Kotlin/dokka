package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.renderers.FileWriter
import org.jetbrains.dokka.renderers.OutputWriter
import org.jetbrains.dokka.resolvers.DefaultLocationProviderFactory

internal object DefaultExtensions {

    private val providerFactory: LazyEvaluated<DefaultLocationProviderFactory> = LazyEvaluated.fromRecipe { DefaultLocationProviderFactory(it) }
    private val outputWriter: LazyEvaluated<OutputWriter> = LazyEvaluated.fromRecipe { FileWriter(it) }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    internal fun <T : Any, E : ExtensionPoint<T>> get(point: E, fullContext: DokkaContext): List<T> =
        when (point) {
            CoreExtensions.locationProviderFactory -> providerFactory.get(fullContext)
            CoreExtensions.outputWriter -> outputWriter.get(fullContext)
            else -> null
        }.let { listOfNotNull( it ) as List<T> }
}