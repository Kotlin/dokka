package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode

/**
 * Implements [LanguageService] and provides rendering of symbols in Kotlin language
 */
class KotlinLanguageService : LanguageService {
    private val fullOnlyModifiers = setOf("public", "protected", "private", "inline", "noinline", "crossinline", "reified")

    override fun render(node: DocumentationNode, renderMode: RenderMode): ContentNode {
        return content {
            when (node.kind) {
                NodeKind.Package -> if (renderMode == RenderMode.FULL) renderPackage(node)
                in NodeKind.classLike -> renderClass(node, renderMode)

                NodeKind.EnumItem,
                NodeKind.ExternalClass -> if (renderMode == RenderMode.FULL) identifier(node.name)

                NodeKind.TypeParameter -> renderTypeParameter(node, renderMode)
                NodeKind.Type,
                NodeKind.UpperBound -> renderType(node, renderMode)

                NodeKind.Modifier -> renderModifier(node)
                NodeKind.Constructor,
                NodeKind.Function,
                NodeKind.CompanionObjectFunction -> renderFunction(node, renderMode)
                NodeKind.Property,
                NodeKind.CompanionObjectProperty -> renderProperty(node, renderMode)
                else -> identifier(node.name)
            }
        }
    }

    override fun renderName(node: DocumentationNode): String {
        return when (node.kind) {
            NodeKind.Constructor -> node.owner!!.name
            else -> node.name
        }
    }

    override fun summarizeSignatures(nodes: List<DocumentationNode>): ContentNode? {
        if (nodes.size < 2) return null
        val receiverKind = nodes.getReceiverKind() ?: return null
        val functionWithTypeParameter = nodes.firstOrNull { it.details(NodeKind.TypeParameter).any() } ?: return null
        return content {
            val typeParameter = functionWithTypeParameter.details(NodeKind.TypeParameter).first()
            if (functionWithTypeParameter.kind == NodeKind.Function) {
                renderFunction(functionWithTypeParameter, RenderMode.SUMMARY, SummarizingMapper(receiverKind, typeParameter.name))
            }
            else {
                renderProperty(functionWithTypeParameter, RenderMode.SUMMARY, SummarizingMapper(receiverKind, typeParameter.name))
            }
        }
    }

    private fun List<DocumentationNode>.getReceiverKind(): ReceiverKind? {
        val qNames = map { it.getReceiverQName() }.filterNotNull()
        if (qNames.size != size)
            return null

        return ReceiverKind.values().firstOrNull { kind -> qNames.all { it in kind.classes } }
    }

    private fun DocumentationNode.getReceiverQName(): String? {
        if (kind != NodeKind.Function && kind != NodeKind.Property) return null
        val receiver = details(NodeKind.Receiver).singleOrNull() ?: return null
        return receiver.detail(NodeKind.Type).qualifiedNameFromType()
    }

    companion object {
        private val arrayClasses = setOf(
                "kotlin.Array",
                "kotlin.BooleanArray",
                "kotlin.ByteArray",
                "kotlin.CharArray",
                "kotlin.ShortArray",
                "kotlin.IntArray",
                "kotlin.LongArray",
                "kotlin.FloatArray",
                "kotlin.DoubleArray"
        )

        private val arrayOrListClasses = setOf("kotlin.List") + arrayClasses

        private val iterableClasses = setOf(
                "kotlin.Collection",
                "kotlin.Sequence",
                "kotlin.Iterable",
                "kotlin.Map",
                "kotlin.String",
                "kotlin.CharSequence") + arrayOrListClasses
    }

    private enum class ReceiverKind(val receiverName: String, val classes: Collection<String>) {
        ARRAY("any_array", arrayClasses),
        ARRAY_OR_LIST("any_array_or_list", arrayOrListClasses),
        ITERABLE("any_iterable", iterableClasses),
    }

    interface SignatureMapper {
        fun renderReceiver(receiver: DocumentationNode, to: ContentBlock)
    }

    private class SummarizingMapper(val kind: ReceiverKind, val typeParameterName: String): SignatureMapper {
        override fun renderReceiver(receiver: DocumentationNode, to: ContentBlock) {
            to.append(ContentIdentifier(kind.receiverName, IdentifierKind.SummarizedTypeName))
            to.text("<$typeParameterName>")
        }
    }

    private fun ContentBlock.renderPackage(node: DocumentationNode) {
        keyword("package")
        text(" ")
        identifier(node.name)
    }

    private fun <T> ContentBlock.renderList(nodes: List<T>, separator: String = ", ",
                                        noWrap: Boolean = false, renderItem: (T) -> Unit) {
        if (nodes.none())
            return
        renderItem(nodes.first())
        nodes.drop(1).forEach {
            if (noWrap) {
                symbol(separator.removeSuffix(" "))
                nbsp()
            } else {
                symbol(separator)
            }
            renderItem(it)
        }
    }

    private fun ContentBlock.renderLinked(node: DocumentationNode, body: ContentBlock.(DocumentationNode)->Unit) {
        val to = node.links.firstOrNull()
        if (to == null)
            body(node)
        else
            link(to) {
                body(node)
            }
    }

    private fun ContentBlock.renderFunctionalTypeParameterName(node: DocumentationNode, renderMode: RenderMode) {
        node.references(RefKind.HiddenAnnotation).map { it.to }
                .find { it.name == "ParameterName" }?.let {
            val parameterNameValue = it.detail(NodeKind.Parameter).detail(NodeKind.Value)
            identifier(parameterNameValue.name.removeSurrounding("\""), IdentifierKind.ParameterName)
            symbol(":")
            nbsp()
        }
    }

    private fun ContentBlock.renderType(node: DocumentationNode, renderMode: RenderMode) {
        var typeArguments = node.details(NodeKind.Type)
        if (node.name == "Function${typeArguments.count() - 1}") {
            // lambda
            val isExtension = node.annotations.any { it.name == "ExtensionFunctionType" }
            if (isExtension) {
                renderType(typeArguments.first(), renderMode)
                symbol(".")
                typeArguments = typeArguments.drop(1)
            }
            symbol("(")
            renderList(typeArguments.take(typeArguments.size - 1), noWrap = true) {
                renderFunctionalTypeParameterName(it, renderMode)
                renderType(it, renderMode)
            }
            symbol(")")
            nbsp()
            symbol("->")
            nbsp()
            renderType(typeArguments.last(), renderMode)
            return
        }
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode, true)
        renderLinked(node) { identifier(it.name, IdentifierKind.TypeName) }
        if (typeArguments.any()) {
            symbol("<")
            renderList(typeArguments, noWrap = true) {
                renderType(it, renderMode)
            }
            symbol(">")
        }
        val nullabilityModifier = node.details(NodeKind.NullabilityModifier).singleOrNull()
        if (nullabilityModifier != null) {
            symbol(nullabilityModifier.name)
        }
    }

    private fun ContentBlock.renderModifier(node: DocumentationNode, nowrap: Boolean = false) {
        when (node.name) {
            "final", "public", "var" -> {}
            else -> {
                keyword(node.name)
                if (nowrap) {
                    nbsp()
                }
                else {
                    text(" ")
                }
            }
        }
    }

    private fun ContentBlock.renderTypeParameter(node: DocumentationNode, renderMode: RenderMode) {
        renderModifiersForNode(node, renderMode, true)

        identifier(node.name)

        val constraints = node.details(NodeKind.UpperBound)
        if (constraints.size == 1) {
            nbsp()
            symbol(":")
            nbsp()
            renderList(constraints, noWrap=true) {
                renderType(it, renderMode)
            }
        }
    }
    private fun ContentBlock.renderParameter(node: DocumentationNode, renderMode: RenderMode) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        identifier(node.name, IdentifierKind.ParameterName, node.detailOrNull(NodeKind.Signature)?.name)
        symbol(":")
        nbsp()
        val parameterType = node.detail(NodeKind.Type)
        renderType(parameterType, renderMode)
        val valueNode = node.details(NodeKind.Value).firstOrNull()
        if (valueNode != null) {
            nbsp()
            symbol("=")
            nbsp()
            text(valueNode.name)
        }
    }

    private fun ContentBlock.renderTypeParametersForNode(node: DocumentationNode, renderMode: RenderMode) {
        val typeParameters = node.details(NodeKind.TypeParameter)
        if (typeParameters.any()) {
            symbol("<")
            renderList(typeParameters) {
                renderTypeParameter(it, renderMode)
            }
            symbol(">")
        }
    }

    private fun ContentBlock.renderExtraTypeParameterConstraints(node: DocumentationNode, renderMode: RenderMode) {
        val parametersWithMultipleConstraints = node.details(NodeKind.TypeParameter).filter { it.details(NodeKind.UpperBound).size > 1 }
        val parametersWithConstraints = parametersWithMultipleConstraints
                .flatMap { parameter -> parameter.details(NodeKind.UpperBound).map { constraint -> parameter to constraint } }
        if (parametersWithMultipleConstraints.isNotEmpty()) {
            keyword(" where ")
            renderList(parametersWithConstraints) {
                identifier(it.first.name)
                nbsp()
                symbol(":")
                nbsp()
                renderType(it.second, renderMode)
            }
        }
    }

    private fun ContentBlock.renderSupertypesForNode(node: DocumentationNode, renderMode: RenderMode) {
        val supertypes = node.details(NodeKind.Supertype)
        if (supertypes.any()) {
            nbsp()
            symbol(":")
            nbsp()
            renderList(supertypes) {
                indentedSoftLineBreak()
                renderType(it, renderMode)
            }
        }
    }

    private fun ContentBlock.renderModifiersForNode(node: DocumentationNode,
                                                    renderMode: RenderMode,
                                                    nowrap: Boolean = false) {
        val modifiers = node.details(NodeKind.Modifier)
        for (it in modifiers) {
            if (node.kind == org.jetbrains.dokka.NodeKind.Interface && it.name == "abstract")
                continue
            if (renderMode == RenderMode.SUMMARY && it.name in fullOnlyModifiers) {
                continue
            }
            renderModifier(it, nowrap)
        }
    }

    private fun ContentBlock.renderAnnotationsForNode(node: DocumentationNode) {
        node.annotations.forEach {
            renderAnnotation(it)
        }
    }

    private fun ContentBlock.renderAnnotation(node: DocumentationNode) {
        identifier("@" + node.name, IdentifierKind.AnnotationName)
        val parameters = node.details(NodeKind.Parameter)
        if (!parameters.isEmpty()) {
            symbol("(")
            renderList(parameters) {
                text(it.detail(NodeKind.Value).name)
            }
            symbol(")")
        }
        text(" ")
    }

    private fun ContentBlock.renderClass(node: DocumentationNode, renderMode: RenderMode) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        when (node.kind) {
            NodeKind.Class,
            NodeKind.AnnotationClass,
            NodeKind.Exception,
            NodeKind.Enum -> keyword("class ")
            NodeKind.Interface -> keyword("interface ")
            NodeKind.EnumItem -> keyword("enum val ")
            NodeKind.Object -> keyword("object ")
            NodeKind.TypeAlias -> keyword("typealias ")
            else -> throw IllegalArgumentException("Node $node is not a class-like object")
        }

        identifierOrDeprecated(node)
        renderTypeParametersForNode(node, renderMode)
        renderSupertypesForNode(node, renderMode)
        renderExtraTypeParameterConstraints(node, renderMode)

        if (node.kind == NodeKind.TypeAlias) {
            nbsp()
            symbol("=")
            nbsp()
            renderType(node.detail(NodeKind.TypeAliasUnderlyingType), renderMode)
        }
    }

    private fun ContentBlock.renderFunction(node: DocumentationNode,
                                            renderMode: RenderMode,
                                            signatureMapper: SignatureMapper? = null) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        when (node.kind) {
            NodeKind.Constructor -> identifier(node.owner!!.name)
            NodeKind.Function,
            NodeKind.CompanionObjectFunction -> keyword("fun ")
            else -> throw IllegalArgumentException("Node $node is not a function-like object")
        }
        renderTypeParametersForNode(node, renderMode)
        if (node.details(NodeKind.TypeParameter).any()) {
            text(" ")
        }

        renderReceiver(node, renderMode, signatureMapper)

        if (node.kind != org.jetbrains.dokka.NodeKind.Constructor)
            identifierOrDeprecated(node)

        symbol("(")
        val parameters = node.details(NodeKind.Parameter)
        renderList(parameters) {
            indentedSoftLineBreak()
            renderParameter(it, renderMode)
        }
        if (needReturnType(node)) {
            if (parameters.isNotEmpty()) {
                softLineBreak()
            }
            symbol(")")
            symbol(": ")
            renderType(node.detail(NodeKind.Type), renderMode)
        }
        else {
            symbol(")")
        }
        renderExtraTypeParameterConstraints(node, renderMode)
    }

    private fun ContentBlock.renderReceiver(node: DocumentationNode, renderMode: RenderMode, signatureMapper: SignatureMapper?) {
        val receiver = node.details(NodeKind.Receiver).singleOrNull()
        if (receiver != null) {
            if (signatureMapper != null) {
                signatureMapper.renderReceiver(receiver, this)
            } else {
                renderType(receiver.detail(NodeKind.Type), renderMode)
            }
            symbol(".")
        }
    }

    private fun needReturnType(node: DocumentationNode) = when(node.kind) {
        NodeKind.Constructor -> false
        else -> !node.isUnitReturnType()
    }

    fun DocumentationNode.isUnitReturnType(): Boolean =
            detail(NodeKind.Type).hiddenLinks.firstOrNull()?.qualifiedName() == "kotlin.Unit"

    private fun ContentBlock.renderProperty(node: DocumentationNode,
                                            renderMode: RenderMode,
                                            signatureMapper: SignatureMapper? = null) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        when (node.kind) {
            NodeKind.Property,
            NodeKind.CompanionObjectProperty -> keyword("${node.getPropertyKeyword()} ")
            else -> throw IllegalArgumentException("Node $node is not a property")
        }
        renderTypeParametersForNode(node, renderMode)
        if (node.details(NodeKind.TypeParameter).any()) {
            text(" ")
        }

        renderReceiver(node, renderMode, signatureMapper)

        identifierOrDeprecated(node)
        symbol(": ")
        renderType(node.detail(NodeKind.Type), renderMode)
        renderExtraTypeParameterConstraints(node, renderMode)
    }

    fun DocumentationNode.getPropertyKeyword() =
            if (details(NodeKind.Modifier).any { it.name == "var" }) "var" else "val"

    fun ContentBlock.identifierOrDeprecated(node: DocumentationNode) {
        if (node.deprecation != null) {
            val strike = ContentStrikethrough()
            strike.identifier(node.name)
            append(strike)
        } else {
            identifier(node.name)
        }
    }
}

fun DocumentationNode.qualifiedNameFromType() = (links.firstOrNull() ?: hiddenLinks.firstOrNull())?.qualifiedName() ?: name
