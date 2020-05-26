package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class MultimoduleLocationProvider(private val root: RootPageNode, context: DokkaContext) : LocationProvider {

    val defaultLocationProvider = DefaultLocationProvider(root, context)

    val paths = context.configuration.modules.map {
        it.name to it.path
    }.toMap()

    override fun resolve(dri: DRI, platforms: List<SourceSetData>, context: PageNode?): String =
        dri.takeIf { it.packageName == MULTIMODULE_PACKAGE_PLACEHOLDER }?.classNames?.let { paths[it] }?.let {
            "$it/${dri.classNames}/index.html"
        } ?: defaultLocationProvider.resolve(dri, platforms, context)

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        defaultLocationProvider.resolve(node, context, skipExtension)

    override fun resolveRoot(node: PageNode): String = defaultLocationProvider.resolveRoot(node)

    override fun ancestors(node: PageNode): List<PageNode> = listOf(root)

    companion object {
        const val MULTIMODULE_PACKAGE_PLACEHOLDER = ".ext"
    }
}