package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.renderers.html.NavigationNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.ContentPage

internal val ContentPage.pageId: String
    get() = pageId(dri.first(), sourceSets())

internal val NavigationNode.pageId: String
    get() = pageId(dri, sourceSets)

@JvmName("shortenSourceSetsToUrl")
internal fun Set<DisplaySourceSet>.shortenToUrl() =
    sortedBy { it.sourceSetIDs.merged.let { it.scopeId + it.sourceSetName } }.joinToString().hashCode()

internal fun DRI.shortenToUrl() = toString()

@JvmName("shortenDrisToUrl")
internal fun Set<DRI>.shortenToUrl() = sortedBy { it.toString() }.joinToString().hashCode()

/**
 * Page Id is required to have a sourceSet in order to distinguish between different pages that has same DRI but different sourceSet
 * like main functions that are not expect/actual
 */
private fun pageId(dri: DRI, sourceSets: Set<DisplaySourceSet>): String = "${dri.shortenToUrl()}/${sourceSets.shortenToUrl()}"