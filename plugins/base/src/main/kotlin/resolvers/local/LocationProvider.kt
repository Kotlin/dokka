package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.PageNode

interface LocationProvider {
    fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode? = null): String?
    fun resolve(node: PageNode, context: PageNode? = null, skipExtension: Boolean = false): String?
    fun pathToRoot(from: PageNode): String
    fun ancestors(node: PageNode): List<PageNode>

    /**
     * This method should return guessed filesystem location for a given [DRI]
     * It is used to decide if a [DRI] should be present in the relocation list of the
     * generated package-list so it is ok if the path differs from the one returned by [resolve]
     * @return Path to a giver [DRI] or null if path should not be considered for relocations
     */
    fun expectedLocationForDri(dri: DRI): String =
        (listOf(dri.packageName) +
                dri.classNames?.split(".")?.map { identifierToFilename(it) }.orEmpty() +
                listOf(dri.callable?.let { identifierToFilename(it.name) } ?: "index")
                ).filterNotNull().joinToString("/")
}

fun LocationProvider.resolveOrThrow(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode? = null): String =
    resolve(dri = dri, sourceSets = sourceSets, context = context)
        ?: throw DokkaException("Cannot resolve path for $dri")

fun LocationProvider.resolveOrThrow(node: PageNode, context: PageNode? = null, skipExtension: Boolean = false): String =
    resolve(node = node, context = context, skipExtension = skipExtension)
        ?: throw DokkaException("Cannot resolve path for ${node.name}")
