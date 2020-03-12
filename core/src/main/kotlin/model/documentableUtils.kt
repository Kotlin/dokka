package org.jetbrains.dokka.model

import org.jetbrains.dokka.pages.PlatformData

fun <T> PlatformDependent<T>.filtered(platformDataList: List<PlatformData>) = PlatformDependent(
    map.filter { it.key in platformDataList },
    expect
)

fun DTypeParameter.filter(filteredData: List<PlatformData>) =
    if (filteredData.containsAll(platformData)) this
    else {
        val intersection = filteredData.intersect(platformData).toList()
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