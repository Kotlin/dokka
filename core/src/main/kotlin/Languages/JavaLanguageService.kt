package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode

/**
 * Implements [LanguageService] and provides rendering of symbols in Java language
 */
class JavaLanguageService : LanguageService {
    override fun render(node: DocumentationNode, renderMode: RenderMode): ContentNode {
        return ContentText(when (node.kind) {
            NodeKind.Package -> renderPackage(node)
            in NodeKind.classLike -> renderClass(node)

            NodeKind.TypeParameter -> renderTypeParameter(node)
            NodeKind.Type,
            NodeKind.UpperBound -> renderType(node)

            NodeKind.Constructor,
            NodeKind.Function -> renderFunction(node)
            NodeKind.Property -> renderProperty(node)
            else -> "${node.kind}: ${node.name}"
        })
    }

    override fun renderName(node: DocumentationNode): String {
        return when (node.kind) {
            NodeKind.Constructor -> node.owner!!.name
            else -> node.name
        }
    }

    override fun summarizeSignatures(nodes: List<DocumentationNode>): ContentNode? = null

    private fun renderPackage(node: DocumentationNode): String {
        return "package ${node.name}"
    }

    private fun renderModifier(node: DocumentationNode): String {
        return when (node.name) {
            "open" -> ""
            "internal" -> ""
            else -> node.name
        }
    }

    fun getArrayElementType(node: DocumentationNode): DocumentationNode? = when (node.qualifiedName()) {
        "kotlin.Array" ->
            node.details(NodeKind.Type).singleOrNull()?.let { et -> getArrayElementType(et) ?: et } ?:
                    DocumentationNode("Object", node.content, NodeKind.ExternalClass)

        "kotlin.IntArray", "kotlin.LongArray", "kotlin.ShortArray", "kotlin.ByteArray",
        "kotlin.CharArray", "kotlin.DoubleArray", "kotlin.FloatArray", "kotlin.BooleanArray" ->
            DocumentationNode(node.name.removeSuffix("Array").toLowerCase(), node.content, NodeKind.Type)

        else -> null
    }

    fun getArrayDimension(node: DocumentationNode): Int = when (node.qualifiedName()) {
        "kotlin.Array" ->
            1 + (node.details(NodeKind.Type).singleOrNull()?.let { getArrayDimension(it) } ?: 0)

        "kotlin.IntArray", "kotlin.LongArray", "kotlin.ShortArray", "kotlin.ByteArray",
        "kotlin.CharArray", "kotlin.DoubleArray", "kotlin.FloatArray", "kotlin.BooleanArray" ->
            1
        else -> 0
    }

    fun renderType(node: DocumentationNode): String {
        return when (node.name) {
            "Unit" -> "void"
            "Int" -> "int"
            "Long" -> "long"
            "Double" -> "double"
            "Float" -> "float"
            "Char" -> "char"
            "Boolean" -> "bool"
        // TODO: render arrays
            else -> node.name
        }
    }

    private fun renderTypeParameter(node: DocumentationNode): String {
        val constraints = node.details(NodeKind.UpperBound)
        return if (constraints.none())
            node.name
        else {
            node.name + " extends " + constraints.joinToString { renderType(node) }
        }
    }

    private fun renderParameter(node: DocumentationNode): String {
        return "${renderType(node.detail(NodeKind.Type))} ${node.name}"
    }

    private fun renderTypeParametersForNode(node: DocumentationNode): String {
        return StringBuilder().apply {
            val typeParameters = node.details(NodeKind.TypeParameter)
            if (typeParameters.any()) {
                append("<")
                append(typeParameters.joinToString { renderTypeParameter(it) })
                append("> ")
            }
        }.toString()
    }

    private fun renderModifiersForNode(node: DocumentationNode): String {
        val modifiers = node.details(NodeKind.Modifier).map { renderModifier(it) }.filter { it != "" }
        if (modifiers.none())
            return ""
        return modifiers.joinToString(" ", postfix = " ")
    }

    private fun renderClass(node: DocumentationNode): String {
        return StringBuilder().apply {
            when (node.kind) {
                NodeKind.Class -> append("class ")
                NodeKind.Interface -> append("interface ")
                NodeKind.Enum -> append("enum ")
                NodeKind.EnumItem -> append("enum value ")
                NodeKind.Object -> append("class ")
                else -> throw IllegalArgumentException("Node $node is not a class-like object")
            }

            append(node.name)
            append(renderTypeParametersForNode(node))
        }.toString()
    }

    private fun renderFunction(node: DocumentationNode): String {
        return StringBuilder().apply {
            when (node.kind) {
                NodeKind.Constructor -> append(node.owner?.name)
                NodeKind.Function -> {
                    append(renderTypeParametersForNode(node))
                    append(renderType(node.detail(NodeKind.Type)))
                    append(" ")
                    append(node.name)
                }
                else -> throw IllegalArgumentException("Node $node is not a function-like object")
            }

            val receiver = node.details(NodeKind.Receiver).singleOrNull()
            append("(")
            if (receiver != null)
                (listOf(receiver) + node.details(NodeKind.Parameter)).joinTo(this) { renderParameter(it) }
            else
                node.details(NodeKind.Parameter).joinTo(this) { renderParameter(it) }

            append(")")
        }.toString()
    }

    private fun renderProperty(node: DocumentationNode): String {
        return StringBuilder().apply {
            when (node.kind) {
                NodeKind.Property -> append("val ")
                else -> throw IllegalArgumentException("Node $node is not a property")
            }
            append(renderTypeParametersForNode(node))
            val receiver = node.details(NodeKind.Receiver).singleOrNull()
            if (receiver != null) {
                append(renderType(receiver.detail(NodeKind.Type)))
                append(".")
            }

            append(node.name)
            append(": ")
            append(renderType(node.detail(NodeKind.Type)))
        }.toString()
    }
}