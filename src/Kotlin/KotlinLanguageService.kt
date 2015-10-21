package org.jetbrains.dokka

import org.jetbrains.dokka.LanguageService.RenderMode

/**
 * Implements [LanguageService] and provides rendering of symbols in Kotlin language
 */
class KotlinLanguageService : LanguageService {
    private val visibilityModifiers = setOf("public", "protected", "private")

    override fun render(node: DocumentationNode, renderMode: RenderMode): ContentNode {
        return content {
            when (node.kind) {
                DocumentationNode.Kind.Package -> if (renderMode == RenderMode.FULL) renderPackage(node)
                DocumentationNode.Kind.Class,
                DocumentationNode.Kind.Interface,
                DocumentationNode.Kind.Enum,
                DocumentationNode.Kind.AnnotationClass,
                DocumentationNode.Kind.Object -> renderClass(node, renderMode)

                DocumentationNode.Kind.EnumItem,
                DocumentationNode.Kind.ExternalClass -> if (renderMode == RenderMode.FULL) identifier(node.name)

                DocumentationNode.Kind.TypeParameter -> renderTypeParameter(node)
                DocumentationNode.Kind.Type,
                DocumentationNode.Kind.UpperBound -> renderType(node)

                DocumentationNode.Kind.Modifier -> renderModifier(node)
                DocumentationNode.Kind.Constructor,
                DocumentationNode.Kind.Function,
                DocumentationNode.Kind.CompanionObjectFunction -> renderFunction(node, renderMode)
                DocumentationNode.Kind.Property,
                DocumentationNode.Kind.CompanionObjectProperty -> renderProperty(node, renderMode)
                else -> identifier(node.name)
            }
        }
    }

    override fun renderName(node: DocumentationNode): String {
        return when (node.kind) {
            DocumentationNode.Kind.Constructor -> node.owner!!.name
            else -> node.name
        }
    }

    private fun ContentBlock.renderPackage(node: DocumentationNode) {
        keyword("package")
        text(" ")
        identifier(node.name)
    }

    private fun ContentBlock.renderList(nodes: List<DocumentationNode>, separator: String = ", ",
                                        noWrap: Boolean = false, renderItem: (DocumentationNode) -> Unit) {
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

    private fun ContentBlock.renderType(node: DocumentationNode) {
        var typeArguments = node.details(DocumentationNode.Kind.Type)
        if (node.name == "Function${typeArguments.count() - 1}") {
            // lambda
            val isExtension = node.annotations.any { it.name == "Extension" }
            if (isExtension) {
                renderType(typeArguments.first())
                symbol(".")
                typeArguments = typeArguments.drop(1)
            }
            symbol("(")
            renderList(typeArguments.take(typeArguments.size - 1), noWrap = true) {
                renderType(it)
            }
            symbol(")")
            nbsp()
            symbol("->")
            nbsp()
            renderType(typeArguments.last())
            return
        }
        renderSingleModifier(node)
        renderLinked(node) { identifier(it.name, IdentifierKind.TypeName) }
        if (typeArguments.any()) {
            symbol("<")
            renderList(typeArguments, noWrap = true) {
                renderType(it)
            }
            symbol(">")
        }
        val nullabilityModifier = node.details(DocumentationNode.Kind.NullabilityModifier).singleOrNull()
        if (nullabilityModifier != null) {
            symbol(nullabilityModifier.name)
        }
    }

    private fun ContentBlock.renderModifier(node: DocumentationNode) {
        when (node.name) {
            "final", "public", "var" -> {}
            else -> {
                keyword(node.name)
                text(" ")
            }
        }
    }

    private fun ContentBlock.renderTypeParameter(node: DocumentationNode) {
        renderSingleModifier(node)

        identifier(node.name)

        val constraints = node.details(DocumentationNode.Kind.UpperBound)
        if (constraints.any()) {
            nbsp()
            symbol(":")
            nbsp()
            renderList(constraints, noWrap=true) {
                renderType(it)
            }
        }
    }

    private fun ContentBlock.renderSingleModifier(node: DocumentationNode) {
        val modifier = node.details(DocumentationNode.Kind.Modifier).singleOrNull()
        if (modifier != null) {
            keyword(modifier.name)
            nbsp()
        }
    }

    private fun ContentBlock.renderParameter(node: DocumentationNode, renderMode: RenderMode) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        identifier(node.name, IdentifierKind.ParameterName)
        symbol(":")
        nbsp()
        val parameterType = node.detail(DocumentationNode.Kind.Type)
        renderType(parameterType)
        val valueNode = node.details(DocumentationNode.Kind.Value).firstOrNull()
        if (valueNode != null) {
            nbsp()
            symbol("=")
            nbsp()
            text(valueNode.name)
        }
    }

    private fun ContentBlock.renderTypeParametersForNode(node: DocumentationNode) {
        val typeParameters = node.details(DocumentationNode.Kind.TypeParameter)
        if (typeParameters.any()) {
            symbol("<")
            renderList(typeParameters) {
                renderTypeParameter(it)
            }
            symbol(">")
        }
    }

    private fun ContentBlock.renderSupertypesForNode(node: DocumentationNode) {
        val supertypes = node.details(DocumentationNode.Kind.Supertype)
        if (supertypes.any()) {
            nbsp()
            symbol(":")
            nbsp()
            renderList(supertypes) {
                renderType(it)
            }
        }
    }

    private fun ContentBlock.renderModifiersForNode(node: DocumentationNode, renderMode: RenderMode) {
        val modifiers = node.details(DocumentationNode.Kind.Modifier)
        for (it in modifiers) {
            if (node.kind == org.jetbrains.dokka.DocumentationNode.Kind.Interface && it.name == "abstract")
                continue
            if (renderMode == RenderMode.SUMMARY && it.name in visibilityModifiers) {
                continue
            }
            renderModifier(it)
        }
    }

    private fun ContentBlock.renderAnnotationsForNode(node: DocumentationNode) {
        node.annotations.forEach {
            renderAnnotation(it)
        }
    }

    private fun ContentBlock.renderAnnotation(node: DocumentationNode) {
        identifier("@" + node.name, IdentifierKind.AnnotationName)
        val parameters = node.details(DocumentationNode.Kind.Parameter)
        if (!parameters.isEmpty()) {
            symbol("(")
            renderList(parameters) {
                text(it.detail(DocumentationNode.Kind.Value).name)
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
            DocumentationNode.Kind.Class,
            DocumentationNode.Kind.AnnotationClass,
            DocumentationNode.Kind.Enum -> keyword("class ")
            DocumentationNode.Kind.Interface -> keyword("interface ")
            DocumentationNode.Kind.EnumItem -> keyword("enum val ")
            DocumentationNode.Kind.Object -> keyword("object ")
            else -> throw IllegalArgumentException("Node $node is not a class-like object")
        }

        identifierOrDeprecated(node)
        renderTypeParametersForNode(node)
        renderSupertypesForNode(node)
    }

    private fun ContentBlock.renderFunction(node: DocumentationNode, renderMode: RenderMode) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        when (node.kind) {
            DocumentationNode.Kind.Constructor -> identifier(node.owner!!.name)
            DocumentationNode.Kind.Function,
            DocumentationNode.Kind.CompanionObjectFunction -> keyword("fun ")
            else -> throw IllegalArgumentException("Node $node is not a function-like object")
        }
        renderTypeParametersForNode(node)
        if (node.details(DocumentationNode.Kind.TypeParameter).any()) {
            text(" ")
        }
        val receiver = node.details(DocumentationNode.Kind.Receiver).singleOrNull()
        if (receiver != null) {
            renderType(receiver.detail(DocumentationNode.Kind.Type))
            symbol(".")
        }

        if (node.kind != org.jetbrains.dokka.DocumentationNode.Kind.Constructor)
            identifierOrDeprecated(node)

        symbol("(")
        renderList(node.details(DocumentationNode.Kind.Parameter)) {
            renderParameter(it, renderMode)
        }
        symbol(")")
        if (needReturnType(node)) {
            symbol(": ")
            renderType(node.detail(DocumentationNode.Kind.Type))
        }
    }

    private fun needReturnType(node: DocumentationNode) = when(node.kind) {
        DocumentationNode.Kind.Constructor -> false
        else -> true
    }

    private fun ContentBlock.renderProperty(node: DocumentationNode, renderMode: RenderMode) {
        if (renderMode == RenderMode.FULL) {
            renderAnnotationsForNode(node)
        }
        renderModifiersForNode(node, renderMode)
        when (node.kind) {
            DocumentationNode.Kind.Property,
            DocumentationNode.Kind.CompanionObjectProperty -> keyword("${node.getPropertyKeyword()} ")
            else -> throw IllegalArgumentException("Node $node is not a property")
        }
        renderTypeParametersForNode(node)
        if (node.details(DocumentationNode.Kind.TypeParameter).any()) {
            text(" ")
        }
        val receiver = node.details(DocumentationNode.Kind.Receiver).singleOrNull()
        if (receiver != null) {
            renderType(receiver.detail(DocumentationNode.Kind.Type))
            symbol(".")
        }

        identifierOrDeprecated(node)
        symbol(": ")
        renderType(node.detail(DocumentationNode.Kind.Type))
    }

    fun DocumentationNode.getPropertyKeyword() =
            if (details(DocumentationNode.Kind.Modifier).any { it.name == "var" }) "var" else "val"

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