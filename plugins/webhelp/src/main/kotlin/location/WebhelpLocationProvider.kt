package org.jetbrains.dokka.webhelp.location

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.*
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
    /**
     * We need to choose something different than `index` since webhelp overrides it
     */
    override val PAGE_WITH_CHILDREN_SUFFIX: String = "webhelp_index"

    override fun pathTo(node: PageNode, context: PageNode?): String =
        pathsIndex[node].orEmpty()
            .let { if (node is ClasslikePageNode || node.children.isNotEmpty()) it + listOf(PAGE_WITH_CHILDREN_SUFFIX) else it }
            .let {
                if (node is ContentPage || node is RootPageNode) "topic/" + it.joinToString(separator = ".")
                else it.joinToString(separator = ".")
            }

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? {
        val location = super.resolve(dri, sourceSets, context)
        return location?.runCatching { URL(this) }?.map { location }
            ?.getOrDefault(location.substringAfter("/"))
    }
}