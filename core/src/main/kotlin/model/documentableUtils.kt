package org.jetbrains.dokka.model

fun <T> SourceSetDependent<T>.filtered(platformDataList: List<SourceSetData>) = SourceSetDependent(
    map.filter { it.key in platformDataList },
    expect
)

fun DTypeParameter.filter(filteredData: List<SourceSetData>) =
    if (filteredData.containsAll(sourceSets)) this
    else {
        val intersection = filteredData.intersect(sourceSets).toList()
        if (intersection.isEmpty()) null
        else DTypeParameter(
            dri,
            name,
            documentation.filtered(intersection),
            bounds,
            intersection,
            extra
        )
    }