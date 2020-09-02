package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.pages.MultimoduleRootPageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaMultiModuleContext
import java.util.concurrent.ConcurrentHashMap

class DokkaLocationProviderFactory(private val context: DokkaMultiModuleContext) : LocationProviderFactory {
    private val cache = ConcurrentHashMap<CacheWrapper, LocationProvider>()

    override fun getLocationProvider(pageNode: RootPageNode) = cache.computeIfAbsent(CacheWrapper(pageNode)) {
        // TODO NOW
        if (pageNode.children.first() is MultimoduleRootPageNode) MultimoduleLocationProvider(pageNode, context)
        else DokkaLocationProvider(pageNode, TODO())
    }

    private class CacheWrapper(val pageNode: RootPageNode) {
        override fun equals(other: Any?) = other is CacheWrapper && other.pageNode == this.pageNode
        override fun hashCode() = System.identityHashCode(pageNode)
    }
}
