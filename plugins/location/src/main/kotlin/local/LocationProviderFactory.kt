package org.jetbrains.dokka.location.local

import org.jetbrains.dokka.location.LocationProvider
import org.jetbrains.dokka.pages.RootPageNode

interface LocationProviderFactory {
    fun getLocationProvider(pageNode: RootPageNode): LocationProvider
}
