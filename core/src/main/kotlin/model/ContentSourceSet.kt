package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.SelfRepresentingSingletonSet

data class ContentSourceSet(
    val sourceSetIDs: CompositeSourceSetID,
    val displayName: String,
    val analysisPlatform: Platform
) : SelfRepresentingSingletonSet<ContentSourceSet> {
    constructor(sourceSet: DokkaSourceSet) : this(
        sourceSetIDs = CompositeSourceSetID(sourceSet.sourceSetID),
        displayName = sourceSet.displayName,
        analysisPlatform = sourceSet.analysisPlatform
    )
}

fun DokkaSourceSet.toContentSourceSet(): ContentSourceSet = ContentSourceSet(this)

fun Iterable<DokkaSourceSet>.toContentSourceSets(): Set<ContentSourceSet> = map { it.toContentSourceSet() }.toSet()

val Iterable<ContentSourceSet>.sourceSetIDs: List<DokkaSourceSetID> get() = this.flatMap { it.sourceSetIDs.all }
