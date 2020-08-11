package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.pages.RootPageNode

interface LocationProviderFactory {
    fun getLocationProvider(pageNode: RootPageNode): LocationProvider
}
