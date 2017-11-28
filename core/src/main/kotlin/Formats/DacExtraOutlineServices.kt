package org.jetbrains.dokka

import com.google.inject.Inject
import java.io.File

class DacExtraOutlineServices @Inject constructor(val locationService: LocationService,
                                                  languageService: LanguageService) :
        ExtraOutlineServices(DacNavOutlineService(locationService, languageService),
                DacSearchOutlineService(locationService, languageService))

class DacNavOutlineService @Inject constructor(val locationService: LocationService,
                                               val languageService: LanguageService) : ExtraOutlineService {
    override fun getFile(location: Location): File  = File("${location.path}.js")

    override fun getFileName(): String = "navtree_data"

    override fun format(node: DocumentationNode): String =
            StringBuilder("var NAVTREE_DATA = ")
                    .appendNavTree(node.members).append(";").toString()

    private fun StringBuilder.appendNavTree(nodes: Iterable<DocumentationNode>): StringBuilder {
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

            append("[ \"${node.name}\", \"${locationService.location(node).path}.html\", [ ")
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

    private fun StringBuilder.appendNavTreePagesOfKind(kindTitle: String,
                                                       nodesOfKind: Iterable<DocumentationNode>): StringBuilder {
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

    private fun StringBuilder.appendNavTreeChild(node: DocumentationNode): StringBuilder {
        append("[ \"${node.nameWithOuterClass()}\", \"${locationService.location(node).path}.html\"")
        append(", null, null, null ]")
        return this
    }
}

class DacSearchOutlineService @Inject constructor(val locationService: LocationService,
                                                  val languageService: LanguageService) : ExtraOutlineService {
    override fun getFile(location: Location): File = File("${location.path}.js")

    override fun getFileName(): String = "lists"

    override fun format(node: DocumentationNode): String {
        val pageNodes = node.getAllPageNodes()
        var id = 0
        val stringBuilder = StringBuilder("var ARCH_DATA = [\n")
        var first = true
        for (pageNode in pageNodes) {
            if (!first) stringBuilder.append(", \n")
            first = false
            stringBuilder.append(" { " +
                    "id:$id, " +
                    "label:\"${pageNode.qualifiedName()}\", " +
                    "link:\"${locationService.location(node).path}.html\", " +
                    "type:\"${pageNode.getClassOrPackage()}\", " +
                    "deprecated:\"false\" }")
            id++
        }
        stringBuilder.append("\n];")
        return stringBuilder.toString()
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