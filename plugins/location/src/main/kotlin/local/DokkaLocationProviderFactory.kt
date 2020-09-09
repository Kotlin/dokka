package org.jetbrains.dokka.location.local

import org.jetbrains.dokka.location.LocationProvider
import org.jetbrains.dokka.pages.MultimoduleRootPageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.concurrent.ConcurrentHashMap

class DokkaLocationProviderFactory(private val context: DokkaContext) : LocationProviderFactory {
    private val cache = ConcurrentHashMap<CacheWrapper, LocationProvider>()

    override fun getLocationProvider(pageNode: RootPageNode) = cache.computeIfAbsent(CacheWrapper(pageNode)) {
        DokkaLocationProvider(pageNode, context)
    }

    private class CacheWrapper(val pageNode: RootPageNode) {
        override fun equals(other: Any?) = other is CacheWrapper && other.pageNode == this.pageNode
        override fun hashCode() = System.identityHashCode(pageNode)
    }
}
