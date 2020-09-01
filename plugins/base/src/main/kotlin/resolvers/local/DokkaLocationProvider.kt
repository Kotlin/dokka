package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.urlEncoded
import java.util.*

open class DokkaLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext,
    extension: String = ".html"
) : DefaultLocationProvider(pageGraphRoot, dokkaContext, extension) {
    protected open val PAGE_WITH_CHILDREN_SUFFIX = "index"

    protected open val pathsIndex: Map<PageNode, List<String>> = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String>) {
            val newPrefix = prefix + page.pathName
            put(page, newPrefix)
            page.children.forEach { registerPath(it, newPrefix) }
        }
        put(pageGraphRoot, emptyList())
        pageGraphRoot.children.forEach { registerPath(it, emptyList()) }
    }

    protected val pagesIndex: Map<Pair<DRI, DisplaySourceSet?>, ContentPage> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.dri.flatMap { dri ->
                    page.sourceSets().ifEmpty { setOf(null) }.map { sourceSet -> (dri to sourceSet) to page }
                }
            }
            .groupingBy { it.first }
            .aggregate { key, _, (_, page), first ->
                if (first) page else throw AssertionError("Multiple pages associated with key: ${key.first}/${key.second}")
            }

    protected val anchorsIndex: Map<Pair<DRI, DisplaySourceSet?>, ContentPage> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.content.withDescendants()
                    .filter { it.extra[SymbolAnchorHint.SymbolAnchorHintKey] != null }
                    .mapNotNull { it.dci.dri.singleOrNull() }
                    .distinct()
                    .flatMap { dri ->
                        page.sourceSets().ifEmpty { setOf(null) }.map { sourceSet ->
                            (dri to sourceSet) to page
                        }
                    }
            }.toMap()

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean) =
        pathTo(node, context) + if (!skipExtension) extension else ""

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? =
        sourceSets.ifEmpty { setOf(null) }.mapNotNull { sourceSet ->
            val driWithSourceSet = Pair(dri, sourceSet)
            getLocalLocation(driWithSourceSet, context)
                ?: getLocalLocation(driWithSourceSet.copy(first = dri.copy(target = PointingToDeclaration)), context)
                // Not found in PageGraph, that means it's an external link
                ?: getExternalLocation(dri, sourceSets)
                ?: getExternalLocation(dri.copy(target = PointingToDeclaration), sourceSets)
        }.distinct().singleOrNull()

    private fun getLocalLocation(dri: Pair<DRI, DisplaySourceSet?>, context: PageNode?): String? =
        pagesIndex[dri]?.let { resolve(it, context) }
            ?: anchorsIndex[dri]?.let { resolve(it, context) + "#${dri.first.toString().urlEncoded()}" }


    override fun pathToRoot(from: PageNode): String =
        pathTo(pageGraphRoot, from).removeSuffix(PAGE_WITH_CHILDREN_SUFFIX)

    override fun ancestors(node: PageNode): List<PageNode> =
        generateSequence(node) { it.parent() }.toList()

    protected open fun pathTo(node: PageNode, context: PageNode?): String {
        fun pathFor(page: PageNode) = pathsIndex[page] ?: throw AssertionError(
            "${page::class.simpleName}(${page.name}) does not belong to the current page graph so it is impossible to compute its path"
        )

        val contextNode =
            if (context !is ClasslikePageNode && context?.children?.isEmpty() == true && context.parent() != null) context.parent() else context
        val nodePath = pathFor(node)
        val contextPath = contextNode?.let { pathFor(it) }.orEmpty()

        val commonPathElements = nodePath.asSequence().zip(contextPath.asSequence())
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) +
                if (node is ClasslikePageNode || node.children.isNotEmpty())
                    listOf(PAGE_WITH_CHILDREN_SUFFIX)
                else
                    emptyList()
                ).joinToString("/")
    }

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]

    private val PageNode.pathName: String
        get() = if (this is PackagePageNode) name else identifierToFilename(name)

    companion object {
        internal val reservedFilenames = setOf("index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out")
        //Taken from: https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
        internal val reservedCharacters = setOf('|', '>', '<', '*', ':', '"', '?', '%')

        internal fun identifierToFilename(name: String): String {
            if (name.isEmpty()) return "--root--"
            return sanitizeFileName(name, reservedFilenames, reservedCharacters)
        }
    }
}

internal fun sanitizeFileName(name: String, reservedFileNames: Set<String>, reservedCharacters: Set<Char>): String {
    val lowercase = name.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    val withoutReservedFileNames = if (lowercase in reservedFileNames) "--$lowercase--" else lowercase
    return reservedCharacters.fold(withoutReservedFileNames){
        acc, character ->
            if(character in acc) acc.replace(character.toString(), "[${character.toInt()}]")
            else acc
    }
}

