package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.CoreExtensions

internal object DefaultExtensions {


    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    internal fun <T : Any, E : ExtensionPoint<T>> get(point: E, fullContext: DokkaContext): List<T> =
        when (point) {
            else -> null
        }.let { listOfNotNull( it ) as List<T> }
}