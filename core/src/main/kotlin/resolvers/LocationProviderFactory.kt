package org.jetbrains.dokka.resolvers

import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaContext

interface LocationProviderFactory {
    fun getLocationProvider(pageNode: PageNode, context: DokkaContext): LocationProvider
}

object DefaultLocationProviderFactory : LocationProviderFactory {
    override fun getLocationProvider(pageNode: PageNode, context: DokkaContext)  = DefaultLocationProvider(pageNode as ModulePageNode, context)
}