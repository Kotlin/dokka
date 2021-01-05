package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.toDisplaySourceSet
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.*

open class DokkaLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext,
    val extension: String = ".html"
) : DokkaBaseLocationProvider(pageGraphRoot, dokkaContext) {
    protected open val PAGE_WITH_CHILDREN_SUFFIX = "index"

    protected open val pathsIndex: Map<PageNode, List<String>> = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String>) {
            if (page is RootPageNode && page.forceTopLevelName) {
                put(page, prefix + PAGE_WITH_CHILDREN_SUFFIX)
                page.children.forEach { registerPath(it, prefix) }
            } else {
                val newPrefix = prefix + page.pathName
                put(page, if (page is ModulePageNode) prefix else newPrefix)
                page.children.forEach { registerPath(it, newPrefix) }
            }

        }
        put(pageGraphRoot, emptyList())
        pageGraphRoot.children.forEach { registerPath(it, emptyList()) }
    }

    protected val pagesIndex: Map<DRIWithSourceSet, ContentPage> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.dri.flatMap { dri ->
                    page.sourceSets().ifEmpty { setOf(null) }.map { sourceSet -> DRIWithSourceSet(dri,sourceSet) to page }
                }
            }
            .groupingBy { it.first }
            .aggregate { key, _, (_, page), first ->
                if (first) page else throw AssertionError("Multiple pages associated with key: ${key.dri}/${key.sourceSet}")
            }

    protected val anchorsIndex: Map<DRIWithSourceSet, PageWithKind> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.content.withDescendants()
                    .filter { it.extra[SymbolAnchorHint] != null && it.dci.dri.any() }
                    .flatMap { content ->
                        content.dci.dri.map { dri ->
                            (dri to content.sourceSets) to content.extra[SymbolAnchorHint]?.contentKind!!
                        }
                    }
                    .distinct()
                    .flatMap { (pair, kind) ->
                        val (dri, sourceSets) = pair
                        sourceSets.ifEmpty { setOf(null) }.map { sourceSet ->
                            DRIWithSourceSet(dri, sourceSet) to PageWithKind(page, kind)
                        }
                    }
            }.toMap()

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean) =
        pathTo(node, context) + if (!skipExtension) extension else ""

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? =
        sourceSets.ifEmpty { setOf(null) }.mapNotNull { sourceSet ->
            val driWithSourceSet = DRIWithSourceSet(dri, sourceSet)
            getLocalLocation(driWithSourceSet, context)
                ?: getLocalLocation(driWithSourceSet.copy(dri = dri.copy(target = PointingToDeclaration)), context)
                // Not found in PageGraph, that means it's an external link
                ?: getExternalLocation(dri, sourceSets)
                ?: getExternalLocation(dri.copy(target = PointingToDeclaration), sourceSets)
        }.distinct().singleOrNull()

    private fun getLocalLocation(driWithSourceSet: DRIWithSourceSet, context: PageNode?): String? {
        val (dri, originalSourceSet) = driWithSourceSet
        val allSourceSets =
            listOf(originalSourceSet) + originalSourceSet?.let { oss ->
                dokkaContext.configuration.sourceSets.filter { it.sourceSetID in oss.sourceSetIDs }
                    .flatMap { it.dependentSourceSets }
                    .mapNotNull { ssid ->
                        dokkaContext.configuration.sourceSets.find { it.sourceSetID == ssid }?.toDisplaySourceSet()
                    }
            }.orEmpty()

        return allSourceSets.asSequence().mapNotNull { displaySourceSet ->
            pagesIndex[DRIWithSourceSet(dri, displaySourceSet)]?.let { page -> resolve(page, context) }
                ?: anchorsIndex[driWithSourceSet]?.let { (page, kind) ->
                    val dci = DCI(setOf(dri), kind)
                    resolve(page, context) + "#" + anchorForDCI(dci, setOfNotNull(displaySourceSet))
                }
        }.firstOrNull()
    }

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

    protected data class DRIWithSourceSet(val dri: DRI, val sourceSet: DisplaySourceSet?)

    protected data class PageWithKind(val page: ContentPage, val kind: Kind)

    companion object {
        val reservedFilenames = setOf("index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out")

        //Taken from: https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
        internal val reservedCharacters = setOf('|', '>', '<', '*', ':', '"', '?', '%')

        fun identifierToFilename(name: String): String {
            if (name.isEmpty()) return "--root--"
            return sanitizeFileName(name, reservedFilenames, reservedCharacters)
        }
    }
}

internal fun sanitizeFileName(name: String, reservedFileNames: Set<String>, reservedCharacters: Set<Char>): String {
    val lowercase = name.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    val withoutReservedFileNames = if (lowercase in reservedFileNames) "--$lowercase--" else lowercase
    return reservedCharacters.fold(withoutReservedFileNames) { acc, character ->
        if (character in acc) acc.replace(character.toString(), "[${character.toInt()}]")
        else acc
    }
}

