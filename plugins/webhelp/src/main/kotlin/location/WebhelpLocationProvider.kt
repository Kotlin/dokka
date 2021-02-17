package org.jetbrains.dokka.webhelp.location

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.net.URL

class WebhelpLocationProviderFactory(private val context: DokkaContext) : LocationProviderFactory {
    override fun getLocationProvider(pageNode: RootPageNode) = WebhelpLocationProvider(pageNode, context)
}

class WebhelpLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : DokkaLocationProvider(pageGraphRoot, dokkaContext, ".xml") {
    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        when (node) {
            is RendererSpecificPage -> super.resolve(node, context, skipExtension)
            is ContentPage -> "topic" + File.separator + super.resolve(node, context, skipExtension)
                .replace(File.separator, ".")
            else -> super.resolve(node, context, skipExtension).replace(File.separator, ".")
        }

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? {
        val location = super.resolve(dri, sourceSets, context)
        return super.resolve(dri, sourceSets, context)
            ?.runCatching { URL(this) }?.map { location }
            ?.getOrDefault(location?.replace(File.separator, ".")?.substringAfter("."))
    }
}