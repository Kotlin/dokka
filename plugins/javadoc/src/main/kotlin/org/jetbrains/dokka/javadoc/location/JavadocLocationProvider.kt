package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProvider
import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Nullable
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.*

class JavadocLocationProvider(pageRoot: RootPageNode, dokkaContext: DokkaContext) :
    DefaultLocationProvider(pageRoot, dokkaContext, ".html") {

    private val pathIndex = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String> = emptyList()) {
            val packagePath = page.takeIf { it is JavadocPackagePageNode }?.name.orEmpty()
                .replace(".", "/")
            val newPathPrefix = prefix + packagePath

            val path = (prefix + when (page) {
                is AllClassesPage -> listOf("allclasses")
                is TreeViewPage -> if (page.classes == null)
                    listOf("overview-tree")
                else
                    listOf("package-tree")
                is ContentPage -> if (page.dri.isNotEmpty() && page.dri.first().classNames != null)
                    listOfNotNull(page.dri.first().classNames)
                else if (page is JavadocPackagePageNode)
                    listOf(packagePath, "package-summary")
                else if (page is IndexPage)
                    listOf("index-files", page.name)
                else if (page is DeprecatedPage)
                    listOf("deprecated")
                else
                    listOf("index")
                else -> emptyList()
            }).filterNot { it.isEmpty() }

            put(page, path)
            page.children.forEach { registerPath(it, newPathPrefix) }

        }
        put(pageRoot, listOf("index"))
        pageRoot.children.forEach { registerPath(it) }
    }

    private val parentPageIndex = HashMap<DRI, PageNode>()
    private val nodeIndex = HashMap<DRI, PageNode>().apply {
        fun registerNode(node: PageNode) {
            if (node is ContentPage) put(node.dri.first(), node)
            (node as? JavadocClasslikePageNode)?.getAnchorables()?.forEach { navigableNode ->
                parentPageIndex[navigableNode.getDRI()] = node
            }
            node.children.forEach(::registerNode)
        }
        registerNode(pageRoot)
    }

    private operator fun IdentityHashMap<PageNode, List<String>>.get(dri: DRI) = this[nodeIndex[dri]]

    private fun List<String>.relativeTo(context: List<String>): String {
        val contextPath = context.dropLast(1)
        val commonPathElements = zip(contextPath).takeWhile { (a, b) -> a == b }.count()
        return (List(contextPath.size - commonPathElements) { ".." } + this.drop(commonPathElements)).joinToString("/")
    }

    private fun JavadocClasslikePageNode.findAnchorableByDRI(dri: DRI): AnchorableJavadocNode? =
        (constructors + methods + entries + properties).firstOrNull { it.dri == dri }

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? =
        getLocalLocation(dri, context)
            ?: getLocalLocation(dri.copy(target = PointingToDeclaration), context)
            // Not found in PageGraph, that means it's an external link
            ?: getExternalLocation(dri, sourceSets)
            ?: getExternalLocation(dri.copy(target = PointingToDeclaration), sourceSets)

    private fun getLocalLocation(dri: DRI, context: PageNode?): String? =
        nodeIndex[dri]?.let { resolve(it, context) }
            ?: parentPageIndex[dri]?.let {
                val anchor = when (val anchorElement = (it as? JavadocClasslikePageNode)?.findAnchorableByDRI(dri)) {
                    is JavadocFunctionNode -> anchorElement.getAnchor()
                    is JavadocEntryNode -> anchorElement.name
                    is JavadocPropertyNode -> anchorElement.name
                    else -> anchorForDri(dri)
                }
                "${resolve(it, context, skipExtension = true)}.html#$anchor"
            }

    private fun anchorForDri(dri: DRI): String =
        dri.callable?.let { callable ->
            "${callable.name}(${
                callable.params.joinToString(",") {
                    ((it as? Nullable)?.wrapped ?: it).toString()
                }
            })"
        } ?: dri.classNames.orEmpty()

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean) =
        pathIndex[node]?.relativeTo(pathIndex[context].orEmpty())?.let {
            if (skipExtension) it.removeSuffix(".html") else it
        } ?: run {
            throw IllegalStateException("Path for ${node::class.java.canonicalName}:${node.name} not found")
        }

    fun resolve(link: LinkJavadocListEntry, contextRoot: PageNode? = null, skipExtension: Boolean = true) =
        pathIndex[link.dri.first()]?.let {
            when (link.kind) {
                JavadocContentKind.Class -> it
                JavadocContentKind.OverviewSummary -> it.dropLast(1) + "index"
                JavadocContentKind.PackageSummary -> it.dropLast(1) + "package-summary"
                JavadocContentKind.AllClasses -> it.dropLast(1) + "allclasses"
                JavadocContentKind.OverviewTree -> it.dropLast(1) + "overview-tree"
                JavadocContentKind.PackageTree -> it.dropLast(1) + "package-tree"
                JavadocContentKind.IndexPage -> it.dropLast(1) + "index-1"
                else -> it
            }
        }?.relativeTo(pathIndex[contextRoot].orEmpty())?.let { if (skipExtension) "$it.html" else it }.orEmpty()

    override fun pathToRoot(from: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}
