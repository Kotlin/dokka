package org.jetbrains.dokka.pages

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


//TODO NOW: Test
data class CompositeSourceSetID(
    private val children: Set<DokkaSourceSetID>
) {
    constructor(sourceSetIDs: Iterable<DokkaSourceSetID>) : this(sourceSetIDs.toSet())
    constructor(sourceSetId: DokkaSourceSetID) : this(setOf(sourceSetId))

    init {
        require(children.isNotEmpty()) { "Expected at least one source set id" }
    }

    val merged = DokkaSourceSetID(
        moduleName = children.map { it.moduleName }.reduce { acc, s -> "$acc+$s" },
        sourceSetName = children.map { it.sourceSetName }.reduce { acc, s -> "$acc+$s" }
    )

    val all: List<DokkaSourceSetID> = listOf(merged, *children.toTypedArray())

    operator fun contains(sourceSetId: DokkaSourceSetID): Boolean {
        return sourceSetId in all
    }

    operator fun contains(sourceSet: DokkaSourceSet): Boolean {
        return sourceSet.sourceSetID in this
    }
}


fun DokkaSourceSet.toContentSourceSet(): ContentSourceSet = ContentSourceSet(this)

fun Iterable<DokkaSourceSet>.toContentSourceSets(): Set<ContentSourceSet> = map { it.toContentSourceSet() }.toSet()

val Iterable<ContentSourceSet>.sourceSetIDs: List<DokkaSourceSetID> get() = this.flatMap { it.sourceSetIDs.all }
