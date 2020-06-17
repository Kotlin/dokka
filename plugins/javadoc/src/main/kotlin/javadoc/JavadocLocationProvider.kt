package javadoc

import javadoc.pages.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import java.nio.file.Paths
import java.util.*

class JavadocLocationProvider(pageRoot: RootPageNode, private val context: DokkaContext) : LocationProvider {
    private val externalLocationProviderFactories =
        context.plugin<DokkaBase>().query { externalLocationProviderFactory }
    private val externalLocationProvider =
        externalLocationProviderFactories.asSequence().map { it.getExternalLocationProvider("javadoc10") }
            .filterNotNull().take(1).firstOrNull()
    private val externalDocumentationLinks by lazy {
        context.configuration.passesConfigurations
            .filter { passConfig -> passConfig.analysisPlatform == Platform.jvm }
            .flatMap { it.externalDocumentationLinks }
            .distinct()
    }

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

    private val nodeIndex = IdentityHashMap<DRI, PageNode>().apply {
        fun registerNode(node: PageNode) {
            if (node is ContentPage) put(node.dri.first(), node)

            node.children.forEach(::registerNode)
        }
        registerNode(pageRoot)
    }

    private operator fun IdentityHashMap<PageNode, List<String>>.get(dri: DRI) = this[nodeIndex[dri]]

    override fun resolve(dri: DRI, sourceSets: List<SourceSetData>, context: PageNode?): String =
        context?.let { resolve(it, skipExtension = false) } ?: nodeIndex[dri]?.let {
            resolve(it, skipExtension = true)
        } ?: with(externalLocationProvider!!) {
            dri.toLocation()
        }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        pathIndex[node]?.joinToString("/")?.let {
            if (skipExtension) it.removeSuffix(".html") else it
        } ?: run {
            throw IllegalStateException("Path for ${node::class.java.canonicalName}:${node.name} not found")
        }

    fun resolve(link: LinkJavadocListEntry, dir: String = "", skipExtension: Boolean = true) = pathIndex[link.dri.first()]?.let {
        when (link.kind) {
            JavadocContentKind.Class -> it
            JavadocContentKind.OverviewSummary -> it.dropLast(1) + "index"
            JavadocContentKind.PackageSummary -> it.dropLast(1) + "package-summary"
            JavadocContentKind.AllClasses -> it.dropLast(1) + "allclasses"
            JavadocContentKind.OverviewTree -> it.dropLast(1) + "overview-tree"
            JavadocContentKind.PackageTree -> it.dropLast(1) + "package-tree"
            else -> it
        }
    }?.joinToString("/")?.let {if (skipExtension) "$it.html" else it}?.let {
        Paths.get(dir).relativize(Paths.get(it)).toString()
    } ?: run {""} //TODO just a glue to compile it on HMPP

    override fun resolveRoot(node: PageNode): String {
        TODO("Not yet implemented")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        TODO("Not yet implemented")
    }
}