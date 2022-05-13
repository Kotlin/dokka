package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet

fun <T> SourceSetDependent<T>.filtered(sourceSets: Set<DokkaSourceSet>) = filter { it.key in sourceSets }
fun DokkaSourceSet?.filtered(sourceSets: Set<DokkaSourceSet>) = takeIf { this in sourceSets }

fun DTypeParameter.filter(filteredSet: Set<DokkaSourceSet>) =
    if (filteredSet.containsAll(sourceSets)) this
    else {
        val intersection = filteredSet.intersect(sourceSets)
        val filteredSources = sources.filtered(intersection)
        if (intersection.isEmpty()) null
        else DTypeParameter(
            variantTypeParameter = variantTypeParameter,
            documentation = documentation.filtered(intersection),
            expectPresentInSet = expectPresentInSet?.takeIf { intersection.contains(expectPresentInSet) },
            bounds = bounds,
            sources = filteredSources,
            sourceSets = intersection,
            extra = extra
        )
    }
