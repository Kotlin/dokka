package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode

/**
 * Implements [LanguageService] and provides rendering of symbols in Java language
 */
class NewJavaLanguageService : CommonLanguageService() {
    override fun showModifierInSummary(node: DocumentationNode): Boolean {
        return true
    }

    override fun render(node: DocumentationNode, renderMode: RenderMode): ContentNode {
        return content {
            (when (node.kind) {
                NodeKind.Package -> renderPackage(node)
                in NodeKind.classLike -> renderClass(node, renderMode)

                NodeKind.Modifier -> renderModifier(this, node, renderMode)
                NodeKind.TypeParameter -> renderTypeParameter(node)
                NodeKind.Type,
                NodeKind.UpperBound -> renderType(node)
                NodeKind.Parameter -> renderParameter(node)
                NodeKind.Constructor,
                NodeKind.Function -> renderFunction(node)
                NodeKind.Property -> renderProperty(node)
                else -> "${node.kind}: ${node.name}"
            })
        }
    }

    override fun summarizeSignatures(nodes: List<DocumentationNode>): ContentNode? = null


    override fun renderModifier(block: ContentBlock, node: DocumentationNode, renderMode: RenderMode, nowrap: Boolean) {
        when (node.name) {
            "open", "internal" -> {
            }
            else -> super.renderModifier(block, node, renderMode, nowrap)
        }
    }

    fun getArrayElementType(node: DocumentationNode): DocumentationNode? = when (node.qualifiedName()) {
        "kotlin.Array" ->
            node.details(NodeKind.Type).singleOrNull()?.let { et -> getArrayElementType(et) ?: et }
                    ?: DocumentationNode("Object", node.content, NodeKind.ExternalClass)

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

    fun ContentBlock.renderType(node: DocumentationNode) {
        when (node.name) {
            "Unit" -> identifier("void")
            "Int" -> identifier("int")
            "Long" -> identifier("long")
            "Double" -> identifier("double")
            "Float" -> identifier("float")
            "Char" -> identifier("char")
            "Boolean" -> identifier("bool")
        // TODO: render arrays
            else -> renderLinked(this, node) {
                identifier(node.name)
            }
        }
    }

    private fun ContentBlock.renderTypeParameter(node: DocumentationNode) {
        val constraints = node.details(NodeKind.UpperBound)
        if (constraints.none())
            identifier(node.name)
        else {
            identifier(node.name)
            text(" ")
            keyword("extends")
            text(" ")
            constraints.forEach { renderType(node) }
        }
    }

    private fun ContentBlock.renderParameter(node: DocumentationNode) {
        renderType(node.detail(NodeKind.Type))
        text(" ")
        identifier(node.name)
    }

    private fun ContentBlock.renderTypeParametersForNode(node: DocumentationNode) {
        val typeParameters = node.details(NodeKind.TypeParameter)
        if (typeParameters.any()) {
            symbol("<")
            renderList(typeParameters, noWrap = true) {
                renderTypeParameter(it)
            }
            symbol(">")
            text(" ")
        }
    }

//    private fun renderModifiersForNode(node: DocumentationNode): String {
//        val modifiers = node.details(NodeKind.Modifier).map { renderModifier(it) }.filter { it != "" }
//        if (modifiers.none())
//            return ""
//        return modifiers.joinToString(" ", postfix = " ")
//    }

    private fun ContentBlock.renderClassKind(node: DocumentationNode) {
        when (node.kind) {
            NodeKind.Interface -> {
                keyword("interface")
            }
            NodeKind.EnumItem -> {
                keyword("enum value")
            }
            NodeKind.Enum -> {
                keyword("enum")
            }
            NodeKind.Class, NodeKind.Exception, NodeKind.Object -> {
                keyword("class")
            }
            else -> throw IllegalArgumentException("Node $node is not a class-like object")
        }
        text(" ")
    }

    private fun ContentBlock.renderClass(node: DocumentationNode, renderMode: RenderMode) {
        renderModifiersForNode(node, renderMode)
        renderClassKind(node)

        identifier(node.name)
        renderTypeParametersForNode(node)
    }

    private fun ContentBlock.renderParameters(nodes: List<DocumentationNode>) {
        renderList(nodes) {
            renderParameter(it)
        }
    }

    private fun ContentBlock.renderFunction(node: DocumentationNode) {
        when (node.kind) {
            NodeKind.Constructor -> identifier(node.owner?.name ?: "")
            NodeKind.Function -> {
                renderTypeParametersForNode(node)
                renderType(node.detail(NodeKind.Type))
                text(" ")
                identifier(node.name)

            }
            else -> throw IllegalArgumentException("Node $node is not a function-like object")
        }

        val receiver = node.details(NodeKind.Receiver).singleOrNull()
        symbol("(")
        if (receiver != null)
            renderParameters(listOf(receiver) + node.details(NodeKind.Parameter))
        else
            renderParameters(node.details(NodeKind.Parameter))

        symbol(")")
    }

    private fun ContentBlock.renderProperty(node: DocumentationNode) {

        when (node.kind) {
            NodeKind.Property -> {
                keyword("val")
                text(" ")
            }
            else -> throw IllegalArgumentException("Node $node is not a property")
        }
        renderTypeParametersForNode(node)
        val receiver = node.details(NodeKind.Receiver).singleOrNull()
        if (receiver != null) {
            renderType(receiver.detail(NodeKind.Type))
            symbol(".")
        }

        identifier(node.name)
        symbol(":")
        text(" ")
        renderType(node.detail(NodeKind.Type))

    }
}