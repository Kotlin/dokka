/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.concurrent.ConcurrentHashMap

public class DokkaLocationProviderFactory(
    private val context: DokkaContext
) : LocationProviderFactory {
    private val cache = ConcurrentHashMap<CacheWrapper, LocationProvider>()

    override fun getLocationProvider(pageNode: RootPageNode): LocationProvider {
        return cache.computeIfAbsent(CacheWrapper(pageNode)) {
            DokkaLocationProvider(pageNode, context)
        }
    }

    private class CacheWrapper(val pageNode: RootPageNode) {
        override fun equals(other: Any?) = other is CacheWrapper && other.pageNode == this.pageNode
        override fun hashCode() = System.identityHashCode(pageNode)
    }
}
