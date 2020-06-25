package javadoc.location

import javadoc.pages.*
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.BaseLocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.*

class JavadocLocationProvider(pageRoot: RootPageNode, private val context: DokkaContext) : BaseLocationProvider(context) {

    private val pathIndex = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String> = emptyList()) {
            val newPrefix = prefix + page.takeIf { it is JavadocPackagePageNode }?.name.orEmpty()
            val path = (prefix + when (page) {
                is AllClassesPage -> listOf("allclasses")
                is TreeViewPage -> if (page.classes == null)
                    listOf("overview-tree")
                else
                    listOf("package-tree")
                is ContentPage -> if (page.dri.isNotEmpty() && page.dri.first().classNames != null)
                    listOfNotNull(page.dri.first().classNames)
                else if (page is JavadocPackagePageNode)
                    listOf(page.name, "package-summary")
                else
                    listOf("index")
                else -> emptyList()
            }).filterNot { it.isEmpty() }

            put(page, path)
            page.children.forEach { registerPath(it, newPrefix) }

        }
        put(pageRoot, listOf("index"))
        pageRoot.children.forEach { registerPath(it) }
    }

    private val nodeIndex = HashMap<DRI, PageNode>().apply {
        fun registerNode(node: PageNode) {
            if (node is ContentPage) put(node.dri.first(), node)
            node.children.forEach(::registerNode)
        }
        registerNode(pageRoot)
    }

    private operator fun IdentityHashMap<PageNode, List<String>>.get(dri: DRI) = this[nodeIndex[dri]]

    private fun List<String>.relativeTo(context: List<String>): String {
        val contextPath = context.dropLast(1)
        val commonPathElements = zip(contextPath).takeWhile { (a,b) -> a == b }.count()
        return (List(contextPath.size - commonPathElements ) { ".." } + this.drop(commonPathElements)).joinToString("/")
    }

    override fun resolve(dri: DRI, sourceSets: Set<DokkaSourceSet>, context: PageNode?): String =
        nodeIndex[dri]?.let { resolve(it, context) }
            ?: getExternalLocation(dri, sourceSets)

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
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
                else -> it
            }
        }?.relativeTo(pathIndex[contextRoot].orEmpty())?.let { if (skipExtension) "$it.html" else it }.orEmpty()

    override fun resolveRoot(node: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}