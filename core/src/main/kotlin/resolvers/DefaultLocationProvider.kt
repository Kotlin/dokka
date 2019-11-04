package org.jetbrains.dokka.resolvers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.htmlEscape
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*

open class DefaultLocationProvider(private val pageGraphRoot: PageNode, val configuration: DokkaConfiguration, val extension: String): LocationProvider { // TODO: cache
    override fun resolve(node: PageNode, context: PageNode?): String = pathTo(node, context) + extension

    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String =
        findInPageGraph(dri, platforms)?.let { resolve(it, context) } ?:
        // Not found in PageGraph, that means it's an external link
        ExternalLocationProvider.getLocation(dri,
            configuration.passesConfigurations
            .filter { passConfig -> passConfig.targets.toSet() == platforms.toSet() } // TODO: change targets to something better?
            .flatMap { it.externalDocumentationLinks }.map { it.packageListUrl }.distinct()
        )

    protected open fun findInPageGraph(dri: DRI, platforms: List<PlatformData>): PageNode? = pageGraphRoot.dfs { it.dri == dri }

    protected open fun pathTo(node: PageNode, context: PageNode?): String {

        fun getPath(pathNode: PageNode?, path: MutableList<String> = mutableListOf()): List<String> =
            if (pathNode == null || pathNode == pageGraphRoot)
                path
            else
                getPath(pathNode.parent, path.apply {
                    add(if (node is PackagePageNode) {
                        node.name
                    } else {
                        identifierToFilename(node.name)
                    })
                })

        val nodePath = getPath(node).reversed()
        val contextPath = getPath(context).reversed()

        val commonPathElements = nodePath.zip(contextPath).takeWhile { (a, b) -> a == b }.size

        return ( List(contextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) ).joinToString("/") +
                if(node.children.isNotEmpty()) PAGE_WITH_CHILDREN_SUFFIX else ""
    }

    private companion object {
        const val PAGE_WITH_CHILDREN_SUFFIX = "/index"
    }
}

fun DRI.toJavadocLocation(jdkVersion: Int): String { // TODO: classes without packages?
    val packageLink = packageName?.replace(".", "/")
    if (classNames == null) {
        return "$packageLink/package-summary.html".htmlEscape()
    }
    val classLink = if (packageLink == null) { "$classNames.html" } else { "$packageLink/$classNames.html" }
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
    val classLink = if (packageName == null) { "" } else { "$packageName/" } +
            classNames.split('.').joinToString("/", transform = ::identifierToFilename)

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
