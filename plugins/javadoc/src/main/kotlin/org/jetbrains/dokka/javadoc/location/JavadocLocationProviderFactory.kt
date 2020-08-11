package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class JavadocLocationProviderFactory(private val context: DokkaContext) : LocationProviderFactory {
    override fun getLocationProvider(pageNode: RootPageNode) =
        JavadocLocationProvider(pageNode, context)
}