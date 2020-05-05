package org.jetbrains.dokka.model

fun <T> SourceSetDependent<T>.filtered(platformDataList: List<SourceSetData>) = filter { it.key in platformDataList }
fun SourceSetData?.filtered(platformDataList: List<SourceSetData>) = takeIf { this in platformDataList }

fun DTypeParameter.filter(filteredData: List<SourceSetData>) =
    if (filteredData.containsAll(sourceSets)) this
    else {
        val intersection = filteredData.intersect(sourceSets).toList()
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