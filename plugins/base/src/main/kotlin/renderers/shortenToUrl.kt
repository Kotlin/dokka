package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet

@JvmName("shortenSourceSetsToUrl")
internal fun Set<DisplaySourceSet>.shortenToUrl() =
    sortedBy { it.sourceSetIDs.merged.let { it.scopeId + it.sourceSetName } }.joinToString().hashCode()

@JvmName("shortenDrisToUrl")
internal fun Set<DRI>.shortenToUrl() = sortedBy { it.toString() }.joinToString().hashCode()
