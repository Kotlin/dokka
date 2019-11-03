package org.jetbrains.dokka.resolvers

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.PlatformData

interface LocationProvider {
    fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode? = null): String
    fun resolve(node: PageNode, context: PageNode? = null): String
}
