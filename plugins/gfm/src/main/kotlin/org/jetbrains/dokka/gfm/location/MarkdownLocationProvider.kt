package org.jetbrains.dokka.gfm.location

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class MarkdownLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : DokkaLocationProvider(pageGraphRoot, dokkaContext, ".md") {

    class Factory(private val context: DokkaContext) : LocationProviderFactory {
        override fun getLocationProvider(pageNode: RootPageNode) =
            MarkdownLocationProvider(pageNode, context)
    }
}

