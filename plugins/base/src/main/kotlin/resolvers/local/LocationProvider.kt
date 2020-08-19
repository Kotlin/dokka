package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.PageNode

interface LocationProvider {
    fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode? = null): String?
    fun resolve(node: PageNode, context: PageNode? = null, skipExtension: Boolean = false): String?
    fun pathToRoot(from: PageNode): String
    fun ancestors(node: PageNode): List<PageNode>
}

fun LocationProvider.resolveOrThrow(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode? = null): String =
    resolve(dri = dri, sourceSets = sourceSets, context = context)
        ?: throw DokkaException("Cannot resolve path for $dri")

fun LocationProvider.resolveOrThrow(node: PageNode, context: PageNode? = null, skipExtension: Boolean = false): String =
    resolve(node = node, context = context, skipExtension = skipExtension)
        ?: throw DokkaException("Cannot resolve path for ${node.name}")
