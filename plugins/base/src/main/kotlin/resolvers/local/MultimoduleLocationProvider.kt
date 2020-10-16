package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class MultimoduleLocationProvider(pageGraphRoot: RootPageNode, dokkaContext: DokkaContext) :
    DokkaBaseLocationProvider(pageGraphRoot, dokkaContext, ".html") {

    private val defaultLocationProvider = DokkaLocationProvider(pageGraphRoot, dokkaContext)

    val paths = dokkaContext.configuration.modules.map {
        it.name to it.relativePathToOutputDirectory
    }.toMap()

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?) =
        dri.takeIf { it.packageName == MULTIMODULE_PACKAGE_PLACEHOLDER }?.classNames?.let { paths[it] }?.let {
            "$it/${identifierToFilename(dri.classNames.orEmpty())}/index.html"
        } ?: defaultLocationProvider.resolve(dri, sourceSets, context)

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean) =
        defaultLocationProvider.resolve(node, context, skipExtension)

    override fun pathToRoot(from: PageNode): String = defaultLocationProvider.pathToRoot(from)

    override fun ancestors(node: PageNode): List<PageNode> = listOf(pageGraphRoot)

    companion object {
        const val MULTIMODULE_PACKAGE_PLACEHOLDER = ".ext"
    }
}
