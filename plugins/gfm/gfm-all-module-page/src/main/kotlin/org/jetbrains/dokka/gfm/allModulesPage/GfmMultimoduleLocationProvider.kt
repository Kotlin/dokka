package org.jetbrains.dokka.gfm.allModulesPage

import org.jetbrains.dokka.allModulesPage.MultimoduleLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.gfm.location.MarkdownLocationProvider
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class GfmMultimoduleLocationProvider(root: RootPageNode, context: DokkaContext) : MultimoduleLocationProvider(root, context, ".md") {

    override val defaultLocationProvider = MarkdownLocationProvider(root, context)

    class Factory(private val context: DokkaContext): LocationProviderFactory {
        override fun getLocationProvider(pageNode: RootPageNode) =
            GfmMultimoduleLocationProvider(pageNode, context)
    }
}