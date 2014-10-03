package org.jetbrains.dokka

import org.jetbrains.dokka.DocumentationNode.*

class KotlinLanguageService : LanguageService {
    override fun render(node: DocumentationNode): ContentNode {
        return content {
            when (node.kind) {
                Kind.Package -> renderPackage(node)
                Kind.Class,
                Kind.Interface,
                Kind.Enum,
                Kind.EnumItem,
                Kind.Object -> renderClass(node)

                Kind.TypeParameter -> renderTypeParameter(node)
                Kind.Type,
                Kind.UpperBound -> renderType(node)

                Kind.Modifier -> renderModifier(node)
                Kind.Constructor,
                Kind.Function -> renderFunction(node)
                Kind.Property -> renderProperty(node)
                else -> ContentText("${node.kind}: ${node.name}")
            }
        }
    }

    override fun renderName(node: DocumentationNode): String {
        return when (node.kind) {
            Kind.Constructor -> node.owner!!.name
            else -> node.name
        }
    }

    fun ContentNode.renderPackage(node: DocumentationNode) {
        keyword("package")
        text(" ")
        identifier(node.name)
    }

    fun ContentNode.renderList(nodes: List<DocumentationNode>, separator: String = ", ", renderItem: (DocumentationNode) -> Unit) {
        if (nodes.none())
            return
        renderItem(nodes.first())
        nodes.drop(1).forEach {
            symbol(separator)
            renderItem(it)
        }
    }

    fun ContentNode.renderLinked(node: DocumentationNode, body: ContentNode.(DocumentationNode)->Unit) {
        val to = node.links.firstOrNull()
        if (to == null)
            body(node)
        else
            link(to) {
                body(node)
            }
    }

    fun ContentNode.renderType(node: DocumentationNode) {
        val typeArguments = node.details(Kind.Type)
        if (node.name == "Function${typeArguments.count() - 1}") {
            // lambda
            symbol("(")
            renderList(typeArguments.take(typeArguments.size - 1)) {
                renderLinked(it) { renderType(it) }
            }
            symbol(")")
            text(" ")
            symbol("->")
            text(" ")
            renderLinked(typeArguments.last()) { renderType(it) }
            return
        }
        if (node.name == "ExtensionFunction${typeArguments.count() - 2}") {
            // extension lambda
            renderLinked(typeArguments.first()) { renderType(it) }
            symbol(".")
            symbol("(")
            renderList(typeArguments.drop(1).take(typeArguments.size - 2)) {
                renderLinked(it) { renderType(it) }
            }
            symbol(")")
            text(" ")
            symbol("->")
            text(" ")
            renderLinked(typeArguments.last()) { renderType(it) }
            return
        }
        identifier(node.name)
        if (typeArguments.any()) {
            symbol("<")
            renderList(typeArguments) {
                renderLinked(it) { renderType(it) }
            }
            symbol(">")
        }
    }

    fun ContentNode.renderModifier(node: DocumentationNode) {
        when (node.name) {
            "final", "internal" -> {
            }
            else -> keyword(node.name)
        }
    }

    fun ContentNode.renderTypeParameter(node: DocumentationNode) {
        val constraints = node.details(Kind.UpperBound)
        identifier(node.name)
        if (constraints.any()) {
            symbol(" : ")
            renderList(constraints) {
                renderLinked(it) { renderType(it) }
            }
        }
    }

    fun ContentNode.renderParameter(node: DocumentationNode) {
        identifier(node.name)
        symbol(": ")
        val parameterType = node.detail(Kind.Type)
        renderLinked(parameterType) { renderType(it) }
    }

    fun ContentNode.renderTypeParametersForNode(node: DocumentationNode) {
        val typeParameters = node.details(Kind.TypeParameter)
        if (typeParameters.any()) {
            symbol("<")
            renderList(typeParameters) {
                renderLinked(it) { renderType(it) }
            }
            symbol("> ")
        }
    }

    fun ContentNode.renderSupertypesForNode(node: DocumentationNode) {
        val supertypes = node.details(Kind.Supertype)
        if (supertypes.any()) {
            symbol(" : ")
            renderList(supertypes) {
                renderLinked(it) { renderType(it) }
            }
        }
    }

    fun ContentNode.renderModifiersForNode(node: DocumentationNode) {
        val modifiers = node.details(Kind.Modifier)
        for (it in modifiers) {
            if (node.kind == Kind.Interface && it.name == "abstract")
                continue
            renderModifier(it)
            text(" ")
        }
    }

    fun ContentNode.renderClass(node: DocumentationNode) {
        renderModifiersForNode(node)
        when (node.kind) {
            Kind.Class -> keyword("class ")
            Kind.Interface -> keyword("trait ")
            Kind.Enum -> keyword("enum class ")
            Kind.EnumItem -> keyword("enum val ")
            Kind.Object -> keyword("object ")
            else -> throw IllegalArgumentException("Node $node is not a class-like object")
        }

        identifier(node.name)
        renderTypeParametersForNode(node)
        renderSupertypesForNode(node)
    }

    fun ContentNode.renderFunction(node: DocumentationNode) {
        renderModifiersForNode(node)
        when (node.kind) {
            Kind.Constructor -> identifier(node.owner!!.name)
            Kind.Function -> keyword("fun ")
            else -> throw IllegalArgumentException("Node $node is not a function-like object")
        }
        renderTypeParametersForNode(node)
        val receiver = node.details(Kind.Receiver).singleOrNull()
        if (receiver != null) {
            renderLinked(receiver.detail(Kind.Type)) { renderType(it) }
            symbol(".")
        }

        if (node.kind != Kind.Constructor)
            identifier(node.name)

        symbol("(")
        renderList(node.details(Kind.Parameter)) {
            renderParameter(it)
        }
        symbol(")")
        if (node.kind != Kind.Constructor) {
            symbol(": ")
            renderLinked(node.detail(Kind.Type)) { renderType(it) }
        }
    }

    fun ContentNode.renderProperty(node: DocumentationNode) {
        renderModifiersForNode(node)
        when (node.kind) {
            Kind.Property -> keyword("val ")
            else -> throw IllegalArgumentException("Node $node is not a property")
        }
        renderTypeParametersForNode(node)
        val receiver = node.details(Kind.Receiver).singleOrNull()
        if (receiver != null) {
            renderLinked(receiver.detail(Kind.Type)) { renderType(it) }
            symbol(".")
        }

        identifier(node.name)
        symbol(": ")
        renderLinked(node.detail(Kind.Type)) { renderType(it) }
    }
}