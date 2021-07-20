package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.pages.OutputExtension
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class JavadocLocationProviderFactory(private val context: DokkaContext) : LocationProviderFactory {
    override fun getLocationProvider(pageNode: RootPageNode, outputExtension: OutputExtension): LocationProvider =
        JavadocLocationProvider(pageNode, context, "." + outputExtension.removePrefix("."))
}