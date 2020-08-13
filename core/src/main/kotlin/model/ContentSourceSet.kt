package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform

data class ContentSourceSet(
    val sourceSetIDs: CompositeSourceSetID,
    val displayName: String,
    val analysisPlatform: Platform
) {
    constructor(sourceSet: DokkaSourceSet) : this(
        sourceSetIDs = CompositeSourceSetID(sourceSet.sourceSetID),
        displayName = sourceSet.displayName,
        analysisPlatform = sourceSet.analysisPlatform
    )

    operator fun contains(sourceSetID: DokkaSourceSetID): Boolean {
        return sourceSetID in sourceSetIDs
    }

    operator fun contains(sourceSet: DokkaSourceSet): Boolean {
        return sourceSet.sourceSetID in this
    }
}


fun DokkaSourceSet.toContentSourceSet(): ContentSourceSet = ContentSourceSet(this)

fun Iterable<DokkaSourceSet>.toContentSourceSets(): Set<ContentSourceSet> = map { it.toContentSourceSet() }.toSet()

val Iterable<ContentSourceSet>.sourceSetIDs: List<DokkaSourceSetID> get() = this.flatMap { it.sourceSetIDs.all }
