package org.jetbrains.dokka.base.resolvers

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.htmlEscape
import java.util.*

private const val PAGE_WITH_CHILDREN_SUFFIX = "index"

open class DefaultLocationProvider(
    protected val pageGraphRoot: RootPageNode,
    protected val dokkaContext: DokkaContext
) : LocationProvider {
    protected open val extension = ".html"

    protected val pagesIndex: Map<DRI, ContentPage> = pageGraphRoot.asSequence().filterIsInstance<ContentPage>()
        .map { it.dri.map { dri -> dri to it } }.flatten()
        .groupingBy { it.first }
        .aggregate { dri, _, (_, page), first ->
            if (first) page else throw AssertionError("Multiple pages associated with dri: $dri")
        }

    protected val pathsIndex: Map<PageNode, List<String>> = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String>) {
            val newPrefix = prefix + page.pathName
            put(page, newPrefix)
            page.children.forEach { registerPath(it, newPrefix) }
        }
        put(pageGraphRoot, emptyList())
        pageGraphRoot.children.forEach { registerPath(it, emptyList()) }
    }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        pathTo(node, context) + if (!skipExtension) extension else ""

    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String =
        pagesIndex[dri]?.let { resolve(it, context) } ?:
        // Not found in PageGraph, that means it's an external link
        ExternalLocationProvider.getLocation(dri,
            this.dokkaContext.configuration.passesConfigurations
                .filter { passConfig ->
                    platforms.toSet()
                        .contains(PlatformData(passConfig.moduleName, passConfig.analysisPlatform, passConfig.targets))
                } // TODO: change targets to something better?
                .flatMap { it.externalDocumentationLinks }.distinct()
        )

    override fun resolveRoot(node: PageNode): String =
        pathTo(pageGraphRoot, node).removeSuffix(PAGE_WITH_CHILDREN_SUFFIX)

    override fun ancestors(node: PageNode): List<PageNode> =
        generateSequence(node) { it.parent() }.toList()

    protected open fun pathTo(node: PageNode, context: PageNode?): String {
        fun pathFor(page: PageNode) = pathsIndex[page] ?: throw AssertionError(
            "${page::class.simpleName}(${page.name}) does not belong to current page graph so it is impossible to compute its path"
        )

        val contextNode =
            if (context?.children?.isEmpty() == true && context.parent() != null) context.parent() else context
        val nodePath = pathFor(node)
        val contextPath = contextNode?.let { pathFor(it) }.orEmpty()

        val commonPathElements = nodePath.asSequence().zip(contextPath.asSequence())
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) +
                if (node.children.isNotEmpty()) listOf(PAGE_WITH_CHILDREN_SUFFIX) else emptyList()).joinToString("/")
    }

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]
}

fun DRI.toJavadocLocation(jdkVersion: Int): String { // TODO: classes without packages?
    val packageLink = packageName?.replace(".", "/")
    if (classNames == null) {
        return "$packageLink/package-summary.html".htmlEscape()
    }
    val classLink = if (packageLink == null) "$classNames.html" else "$packageLink/$classNames.html"
    val callableChecked = callable ?: return classLink.htmlEscape()

    val callableLink = "$classLink#${callableChecked.name}" + when {
        jdkVersion < 8 -> "(${callableChecked.params.joinToString(", ")})"
        jdkVersion < 10 -> "-${callableChecked.params.joinToString("-")}-"
        else -> "(${callableChecked.params.joinToString(",")})"
    }

    return callableLink.htmlEscape()
}

fun DRI.toDokkaLocation(extension: String): String { // TODO: classes without packages?
    val classNamesChecked = classNames ?: return "$packageName/index$extension"

    val classLink = if (packageName == null) {
        ""
    } else {
        "$packageName/"
    } + classNamesChecked.split('.').joinToString("/", transform = ::identifierToFilename)

    val callableChecked = callable ?: return "$classLink/index$extension"

    return "$classLink/${identifierToFilename(callableChecked.name)}$extension"
}

private val reservedFilenames = setOf("index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out")

private fun identifierToFilename(name: String): String {
    if (name.isEmpty()) return "--root--"
    val escaped = name.replace('<', '-').replace('>', '-')
    val lowercase = escaped.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    return if (lowercase in reservedFilenames) "--$lowercase--" else lowercase
}

private val PageNode.pathName: String
    get() = if (this is PackagePageNode) name else identifierToFilename(name)
