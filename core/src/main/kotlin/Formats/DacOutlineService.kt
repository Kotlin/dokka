package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import org.jetbrains.dokka.*
import java.net.URI
import com.google.inject.name.Named
import org.jetbrains.kotlin.cfg.pseudocode.AllTypes


interface DacOutlineFormatService {
    fun computeOutlineURI(node: DocumentationNode): URI
    fun format(to: Appendable, node: DocumentationNode)
}

class DacOutlineFormatter @Inject constructor(
        uriProvider: JavaLayoutHtmlUriProvider,
        languageService: LanguageService,
        @Named("dacRoot") dacRoot: String,
        @Named("generateClassIndex") generateClassIndex: Boolean,
        @Named("generatePackageIndex") generatePackageIndex: Boolean
) : JavaLayoutHtmlFormatOutlineFactoryService {
    val tocOutline = TocOutlineService(uriProvider, languageService, dacRoot, generateClassIndex, generatePackageIndex)
    val outlines = listOf(tocOutline)

    override fun generateOutlines(outputProvider: (URI) -> Appendable, nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            for (outline in outlines) {
                val uri = outline.computeOutlineURI(node)
                val output = outputProvider(uri)
                outline.format(output, node)
            }
        }
    }
}

/**
 * Outline service for generating a _toc.yaml file, responsible for pointing to the paths of each
 * index.html file in the doc tree.
 */
class BookOutlineService(
        val uriProvider: JavaLayoutHtmlUriProvider,
        val languageService: LanguageService,
        val dacRoot: String,
        val generateClassIndex: Boolean,
        val generatePackageIndex: Boolean
) : DacOutlineFormatService {
    override fun computeOutlineURI(node: DocumentationNode): URI = uriProvider.outlineRootUri(node).resolve("_book.yaml")

    override fun format(to: Appendable, node: DocumentationNode) {
        appendOutline(to, listOf(node))
    }

    var outlineLevel = 0

    /** Appends formatted outline to [StringBuilder](to) using specified [location] */
    fun appendOutline(to: Appendable, nodes: Iterable<DocumentationNode>) {
        if (outlineLevel == 0) to.appendln("reference:")
        for (node in nodes) {
            appendOutlineHeader(node, to)
            val subPackages = node.members.filter {
                it.kind == NodeKind.Package
            }
            if (subPackages.any()) {
                val sortedMembers = subPackages.sortedBy { it.name.toLowerCase() }
                appendOutlineLevel(to) {
                    appendOutline(to, sortedMembers)
                }
            }

        }
    }

    fun appendOutlineHeader(node: DocumentationNode, to: Appendable) {
        if (node is DocumentationModule) {
            to.appendln("- title: Package Index")
            to.appendln("  path: $dacRoot${uriProvider.outlineRootUri(node).resolve("packages.html")}")
            to.appendln("  status_text: no-toggle")
        } else {
            to.appendln("- title: ${languageService.renderName(node)}")
            to.appendln("  path: $dacRoot${uriProvider.mainUriOrWarn(node)}")
            to.appendln("  status_text: no-toggle")
        }
    }

    fun appendOutlineLevel(to: Appendable, body: () -> Unit) {
        outlineLevel++
        body()
        outlineLevel--
    }
}

/**
 * Outline service for generating a _toc.yaml file, responsible for pointing to the paths of each
 * index.html file in the doc tree.
 */
class TocOutlineService(
        val uriProvider: JavaLayoutHtmlUriProvider,
        val languageService: LanguageService,
        val dacRoot: String,
        val generateClassIndex: Boolean,
        val generatePackageIndex: Boolean
) : DacOutlineFormatService {
    override fun computeOutlineURI(node: DocumentationNode): URI = uriProvider.outlineRootUri(node).resolve("_toc.yaml")

    override fun format(to: Appendable, node: DocumentationNode) {
        appendOutline(to, listOf(node))
    }

    var outlineLevel = 0

    /** Appends formatted outline to [StringBuilder](to) using specified [location] */
    fun appendOutline(to: Appendable, nodes: Iterable<DocumentationNode>) {
        if (outlineLevel == 0) to.appendln("toc:")
        for (node in nodes) {
            appendOutlineHeader(node, to)
            val subPackages = node.members.filter {
                it.kind == NodeKind.Package
            }
            if (subPackages.any()) {
                val sortedMembers = subPackages.sortedBy { it.nameWithOuterClass() }
                appendOutlineLevel {
                    appendOutline(to, sortedMembers)
                }
            }
        }
    }

    fun appendOutlineHeader(node: DocumentationNode, to: Appendable) {
        if (node is DocumentationModule) {
            if (generateClassIndex) {
                node.members.filter { it.kind == NodeKind.AllTypes }.firstOrNull()?.let {
                    to.appendln("- title: Class Index")
                    to.appendln("  path: $dacRoot${uriProvider.outlineRootUri(it).resolve("classes.html")}")
                    to.appendln()
                }
            }
            if (generatePackageIndex) {
                to.appendln("- title: Package Index")
                to.appendln("  path: $dacRoot${uriProvider.outlineRootUri(node).resolve("packages.html")}")
                to.appendln()
            }
        } else if (node.kind != NodeKind.AllTypes && !(node is DocumentationModule)) {
            to.appendln("- title: ${languageService.renderName(node)}")
            to.appendln("  path: $dacRoot${uriProvider.mainUriOrWarn(node)}")
            to.appendln()
            var addedSectionHeader = false
            for (kind in NodeKind.classLike) {
                val members = node.getMembersOfKinds(kind)
                if (members.isNotEmpty()) {
                    if (!addedSectionHeader) {
                        to.appendln("  section:")
                        addedSectionHeader = true
                    }
                    to.appendln("  - title: ${kind.pluralizedName()}")
                    to.appendln()
                    to.appendln("    section:")
                    members.sortedBy { it.nameWithOuterClass().toLowerCase() }.forEach { member ->
                        to.appendln("    - title: ${languageService.renderNameWithOuterClass(member)}")
                        to.appendln("      path: $dacRoot${uriProvider.mainUriOrWarn(member)}".trimEnd('#'))
                        to.appendln()
                    }
                }
            }
            to.appendln().appendln()
        }
    }

    fun appendOutlineLevel(body: () -> Unit) {
        outlineLevel++
        body()
        outlineLevel--
    }
}

class DacNavOutlineService constructor(
        val uriProvider: JavaLayoutHtmlUriProvider,
        val languageService: LanguageService,
        val dacRoot: String
) : DacOutlineFormatService {
    override fun computeOutlineURI(node: DocumentationNode): URI =
            uriProvider.outlineRootUri(node).resolve("navtree_data.js")

    override fun format(to: Appendable, node: DocumentationNode) {
        to.append("var NAVTREE_DATA = ").appendNavTree(node.members).append(";")
    }

    private fun Appendable.appendNavTree(nodes: Iterable<DocumentationNode>): Appendable {
        append("[ ")
        var first = true
        for (node in nodes) {
            if (!first) append(", ")
            first = false
            val interfaces = node.getMembersOfKinds(NodeKind.Interface)
            val classes = node.getMembersOfKinds(NodeKind.Class)
            val objects = node.getMembersOfKinds(NodeKind.Object)
            val annotations = node.getMembersOfKinds(NodeKind.AnnotationClass)
            val enums = node.getMembersOfKinds(NodeKind.Enum)
            val exceptions = node.getMembersOfKinds(NodeKind.Exception)

            append("[ \"${node.name}\", \"$dacRoot${uriProvider.tryGetMainUri(node)}\", [ ")
            var needComma = false
            if (interfaces.firstOrNull() != null) {
                appendNavTreePagesOfKind("Interfaces", interfaces)
                needComma = true
            }
            if (classes.firstOrNull() != null) {
                if (needComma) append(", ")
                appendNavTreePagesOfKind("Classes", classes)
                needComma = true
            }
            if (objects.firstOrNull() != null) {
                if (needComma) append(", ")
                appendNavTreePagesOfKind("Objects", objects)
            }
            if (annotations.firstOrNull() != null) {
                if (needComma) append(", ")
                appendNavTreePagesOfKind("Annotations", annotations)
                needComma = true
            }
            if (enums.firstOrNull() != null) {
                if (needComma) append(", ")
                appendNavTreePagesOfKind("Enums", enums)
                needComma = true
            }
            if (exceptions.firstOrNull() != null) {
                if (needComma) append(", ")
                appendNavTreePagesOfKind("Exceptions", exceptions)
            }
            append(" ] ]")
        }
        append(" ]")
        return this
    }

    private fun Appendable.appendNavTreePagesOfKind(kindTitle: String,
                                                    nodesOfKind: Iterable<DocumentationNode>): Appendable {
        append("[ \"$kindTitle\", null, [ ")
        var started = false
        for (node in nodesOfKind) {
            if (started) append(", ")
            started = true
            appendNavTreeChild(node)
        }
        append(" ], null, null ]")
        return this
    }

    private fun Appendable.appendNavTreeChild(node: DocumentationNode): Appendable {
        append("[ \"${node.nameWithOuterClass()}\", \"${dacRoot}${uriProvider.tryGetMainUri(node)}\"")
        append(", null, null, null ]")
        return this
    }
}

class DacSearchOutlineService(
        val uriProvider: JavaLayoutHtmlUriProvider,
        val languageService: LanguageService,
        val dacRoot: String
) : DacOutlineFormatService {

    override fun computeOutlineURI(node: DocumentationNode): URI =
            uriProvider.outlineRootUri(node).resolve("lists.js")

    override fun format(to: Appendable, node: DocumentationNode) {
        val pageNodes = node.getAllPageNodes()
        var id = 0
        to.append("var KTX_CORE_DATA = [\n")
        var first = true
        for (pageNode in pageNodes) {
            if (pageNode.kind == NodeKind.Module) continue
            if (!first) to.append(", \n")
            first = false
            to.append(" { " +
                    "id:$id, " +
                    "label:\"${pageNode.qualifiedName()}\", " +
                    "link:\"${dacRoot}${uriProvider.tryGetMainUri(pageNode)}\", " +
                    "type:\"${pageNode.getClassOrPackage()}\", " +
                    "deprecated:\"false\" }")
            id++
        }
        to.append("\n];")
    }

    private fun DocumentationNode.getClassOrPackage(): String =
            if (hasOwnPage())
                "class"
            else if (isPackage()) {
                "package"
            } else {
                ""
            }

    private fun DocumentationNode.getAllPageNodes(): Iterable<DocumentationNode> {
        val allPageNodes = mutableListOf<DocumentationNode>()
        recursiveSetAllPageNodes(allPageNodes)
        return allPageNodes
    }

    private fun DocumentationNode.recursiveSetAllPageNodes(
            allPageNodes: MutableList<DocumentationNode>) {
        for (child in members) {
            if (child.hasOwnPage() || child.isPackage()) {
                allPageNodes.add(child)
                child.qualifiedName()
                child.recursiveSetAllPageNodes(allPageNodes)
            }
        }
    }

}

/**
 * Return all children of the node who are one of the selected `NodeKind`s. It recursively fetches
 * all offspring, not just immediate children.
 */
fun DocumentationNode.getMembersOfKinds(vararg kinds: NodeKind): MutableList<DocumentationNode> {
    val membersOfKind = mutableListOf<DocumentationNode>()
    recursiveSetMembersOfKinds(kinds, membersOfKind)
    return membersOfKind
}

private fun DocumentationNode.recursiveSetMembersOfKinds(kinds: Array<out NodeKind>,
                                                         membersOfKind: MutableList<DocumentationNode>) {
    for (member in members) {
        if (member.kind in kinds) {
            membersOfKind.add(member)
        }
        member.recursiveSetMembersOfKinds(kinds, membersOfKind)
    }
}

/**
 * Returns whether or not this node owns a page. The criteria for whether a node owns a page is
 * similar to the way javadoc is structured. Classes, Interfaces, Enums, AnnotationClasses,
 * Exceptions, and Objects (Kotlin-specific) meet the criteria.
 */
fun DocumentationNode.hasOwnPage() =
        kind == NodeKind.Class || kind == NodeKind.Interface || kind == NodeKind.Enum ||
                kind == NodeKind.AnnotationClass || kind == NodeKind.Exception ||
                kind == NodeKind.Object

/**
 * In most cases, this returns the short name of the `Type`. When the Type is an inner Type, it
 * prepends the name with the containing Type name(s).
 *
 * For example, if you have a class named OuterClass and an inner class named InnerClass, this would
 * return OuterClass.InnerClass.
 *
 */
fun DocumentationNode.nameWithOuterClass(): String {
    val nameBuilder = StringBuilder(name)
    var parent = owner
    if (hasOwnPage()) {
        while (parent != null && parent.hasOwnPage()) {
            nameBuilder.insert(0, "${parent.name}.")
            parent = parent.owner
        }
    }
    return nameBuilder.toString()
}

/**
 * Return whether the node is a package.
 */
fun DocumentationNode.isPackage(): Boolean {
    return kind == NodeKind.Package
}

/**
 * Return the 'page owner' of this node. `DocumentationNode.hasOwnPage()` defines the criteria for
 * a page owner. If this node is not a page owner, then it iterates up through its ancestors to
 * find the first page owner.
 */
fun DocumentationNode.pageOwner(): DocumentationNode {
    if (hasOwnPage() || owner == null) {
        return this
    } else {
        var parent: DocumentationNode = owner!!
        while (!parent.hasOwnPage() && !parent.isPackage()) {
            parent = parent.owner!!
        }
        return parent
    }
}

fun NodeKind.pluralizedName() = when(this) {
    NodeKind.Class -> "Classes"
    NodeKind.Interface -> "Interfaces"
    NodeKind.AnnotationClass -> "Annotations"
    NodeKind.Enum -> "Enums"
    NodeKind.Exception -> "Exceptions"
    NodeKind.Object -> "Objects"
    NodeKind.TypeAlias -> "TypeAliases"
    else -> "${name}s"
}