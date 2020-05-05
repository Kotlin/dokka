package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode

interface LocationProvider {
    fun resolve(dri: DRI, sourceSets: List<SourceSetData>, context: PageNode? = null): String
    fun resolve(node: PageNode, context: PageNode? = null, skipExtension: Boolean = false): String
    fun resolveRoot(node: PageNode): String
    fun ancestors(node: PageNode): List<PageNode>
}

interface LocationProviderFactory {
    fun getLocationProvider(pageNode: RootPageNode): LocationProvider
}

