package org.jetbrains.dokka.resolvers

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.utilities.htmlEscape
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.single

open class DefaultLocationProvider(
    private val pageGraphRoot: ModulePageNode,
    private val configuration: DokkaConfiguration,
    context: DokkaContext
) : LocationProvider { // TODO: cache
    private val extension = context.single(CoreExtensions.fileExtension)

    override fun resolve(node: PageNode, context: PageNode?): String = pathTo(node, context) + extension

    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String =
        findInPageGraph(dri, platforms)?.let { resolve(it, context) } ?:
        // Not found in PageGraph, that means it's an external link
        ExternalLocationProvider.getLocation(dri,
            configuration.passesConfigurations
                .filter { passConfig ->
                    platforms.toSet().contains(PlatformData(passConfig.analysisPlatform, passConfig.targets))
                } // TODO: change targets to something better?
                .flatMap { it.externalDocumentationLinks }.distinct()
        )

    override fun resolveRoot(node: PageNode): String = "../${pathTo(pageGraphRoot, node).removeSuffix(
        PAGE_WITH_CHILDREN_SUFFIX
    )}"

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]

    override fun ancestors(node: PageNode?): List<PageNode> = when (node) {
        null -> emptyList()
        else -> ancestors(node.parent()) + node
    }

    protected open fun findInPageGraph(dri: DRI, platforms: List<PlatformData>): PageNode? =
        pageGraphRoot.dfs { it.dri == dri }

    protected open fun pathTo(node: PageNode, context: PageNode?): String {

        fun PageNode.pathName(): String =
            if (this is PackagePageNode) name
            else identifierToFilename(name)

        fun getPath(pathNode: PageNode?, path: List<String> = mutableListOf()): List<String> = when (pathNode) {
            null -> path
            pageGraphRoot -> path + "root"
            else -> getPath(pathNode.parent(), path + pathNode.pathName())
        }

        val contextNode = if (context?.children?.isEmpty() == true) context.parent() else context
        val nodePath = getPath(node).reversed()
        val contextPath = getPath(contextNode).reversed()

        val commonPathElements = nodePath.zip(contextPath).takeWhile { (a, b) -> a == b }.size

        return (List(contextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) +
                if (node.children.isNotEmpty()) listOf(PAGE_WITH_CHILDREN_SUFFIX) else emptyList()).joinToString("/")
    }

    private companion object {
        const val PAGE_WITH_CHILDREN_SUFFIX = "index"
    }
}

fun DRI.toJavadocLocation(jdkVersion: Int): String { // TODO: classes without packages?
    val packageLink = packageName?.replace(".", "/")
    if (classNames == null) {
        return "$packageLink/package-summary.html".htmlEscape()
    }
    val classLink = if (packageLink == null) "$classNames.html" else "$packageLink/$classNames.html"
    if (callable == null) {
        return classLink.htmlEscape()
    }

    val callableLink = "$classLink#${callable.name}" + when {
        jdkVersion < 8 -> "(${callable.params.joinToString(", ")})"
        jdkVersion < 10 -> "-${callable.params.joinToString("-")}-"
        else -> "(${callable.params.joinToString(",")})"
    }

    return callableLink.htmlEscape()
}

fun DRI.toDokkaLocation(extension: String): String { // TODO: classes without packages?
    if (classNames == null) {
        return "$packageName/index$extension"
    }
    val classLink = if (packageName == null) {
        ""
    } else {
        "$packageName/"
    } + classNames.split('.').joinToString("/", transform = ::identifierToFilename)

    if (callable == null) {
        return "$classLink/index$extension"
    }

    return "$classLink/${identifierToFilename(callable.name)}$extension"
}

private val reservedFilenames = setOf("index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out")

private fun identifierToFilename(name: String): String {
    if (name.isEmpty()) return "--root--"
    val escaped = name.replace('<', '-').replace('>', '-')
    val lowercase = escaped.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    return if (lowercase in reservedFilenames) "--$lowercase--" else lowercase
}