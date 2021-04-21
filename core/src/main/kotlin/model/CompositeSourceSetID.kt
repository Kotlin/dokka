package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID

data class CompositeSourceSetID(
    private val children: Set<DokkaSourceSetID>
) {
    constructor(sourceSetIDs: Iterable<DokkaSourceSetID>) : this(sourceSetIDs.toSet())
    constructor(sourceSetId: DokkaSourceSetID) : this(setOf(sourceSetId))

    init {
        require(children.isNotEmpty()) { "Expected at least one source set id" }
    }

    val merged: DokkaSourceSetID
        get() = children.sortedBy { it.scopeId + it.sourceSetName }.let { sortedChildren ->
            DokkaSourceSetID(
                scopeId = sortedChildren.joinToString(separator = "+") { it.scopeId },
                sourceSetName = sortedChildren.joinToString(separator = "+") { it.sourceSetName }
            )
        }

    val all: Set<DokkaSourceSetID>
        get() = setOf(merged, *children.toTypedArray())

    operator fun contains(sourceSetId: DokkaSourceSetID): Boolean {
        return sourceSetId in all
    }

    operator fun contains(sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        return sourceSet.sourceSetID in this
    }

    operator fun plus(other: DokkaSourceSetID): CompositeSourceSetID {
        return copy(children = children + other)
    }
}

operator fun DokkaSourceSetID.plus(other: DokkaSourceSetID): CompositeSourceSetID {
    return CompositeSourceSetID(listOf(this, other))
}
