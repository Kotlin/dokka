package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.pages.MultimoduleRootPageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class DefaultLocationProviderFactory(private val context: DokkaContext) : LocationProviderFactory {

    override fun getLocationProvider(pageNode: RootPageNode) =
        if (pageNode.children.first() is MultimoduleRootPageNode) MultimoduleLocationProvider(pageNode, context)
        else DefaultLocationProvider(pageNode, context)
}