package org.jetbrains.dokka.resolvers

import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext

interface LocationProviderFactory {
    fun getLocationProvider(pageNode: ModulePageNode): LocationProvider
}

class DefaultLocationProviderFactory(val context: DokkaContext) : LocationProviderFactory {

    override fun getLocationProvider(pageNode: ModulePageNode)  = DefaultLocationProvider(pageNode, context)
}