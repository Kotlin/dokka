package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.SelfRepresentingSingletonSet

/**
 * TODO: fix the example (asymmetric equivalence relation with [Set]):
 * ```
 * val ds = DokkaSourceSetImpl(sourceSetID = DokkaSourceSetID("", "")).toDisplaySourceSet()
 * println(setOf(ds) == ds) // true
 * println(ds == setOf(ds)) // false
 * ```
 */
data class DisplaySourceSet(
    val sourceSetIDs: CompositeSourceSetID,
    val name: String,
    val platform: Platform
) : SelfRepresentingSingletonSet<DisplaySourceSet> {
    constructor(sourceSet: DokkaSourceSet) : this(
        sourceSetIDs = CompositeSourceSetID(sourceSet.sourceSetID),
        name = sourceSet.displayName,
        platform = sourceSet.analysisPlatform
    )
}

fun DokkaSourceSet.toDisplaySourceSet(): DisplaySourceSet = DisplaySourceSet(this)

fun Iterable<DokkaSourceSet>.toDisplaySourceSets(): Set<DisplaySourceSet> = map { it.toDisplaySourceSet() }.toSet()

val Iterable<DisplaySourceSet>.sourceSetIDs: List<DokkaSourceSetID> get() = this.flatMap { it.sourceSetIDs.all }
