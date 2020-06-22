package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet

fun <T> SourceSetDependent<T>.filtered(sourceSets: Set<DokkaSourceSet>) = filter { it.key in sourceSets }
fun DokkaSourceSet?.filtered(sourceSets: Set<DokkaSourceSet>) = takeIf { this in sourceSets }

fun DTypeParameter.filter(filteredSet: Set<DokkaSourceSet>) =
    if (filteredSet.containsAll(sourceSets)) this
    else {
        val intersection = filteredSet.intersect(sourceSets)
        if (intersection.isEmpty()) null
        else DTypeParameter(
            dri,
            name,
            documentation.filtered(intersection),
            expectPresentInSet?.takeIf { intersection.contains(expectPresentInSet) },
            bounds,
            intersection,
            extra
        )
    }

interface WithChildren {
    val children: List<*>
}

inline fun <reified T> WithChildren.firstChildOfType() =
    children.filterIsInstance<T>().firstOrNull()

inline fun <reified T> WithChildren.firstChildOfType(predicate: (T) -> Boolean) =
    children.filterIsInstance<T>().firstOrNull(predicate)