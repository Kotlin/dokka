package org.jetbrains.dokka

import org.jetbrains.dokka.DocumentationNode.*

class KotlinSignatureGenerator : SignatureGenerator {
    override fun render(node: DocumentationNode): String {
        return when (node.kind) {
            Kind.Package -> renderPackage(node)
            Kind.Class,
            Kind.Trait,
            Kind.Object -> renderClass(node)

            Kind.TypeParameter -> renderTypeParameter(node)
            Kind.Type,
            Kind.UpperBound -> renderType(node)

            Kind.Constructor,
            Kind.Function -> renderFunction(node)
            Kind.Property -> renderProperty(node)
            else -> "${node.kind}: ${node.name}"
        }
    }

    override fun renderPackage(node: DocumentationNode): String {
        return "package ${node.name}"
    }

    override fun renderType(node: DocumentationNode): String {
        return node.name
    }

    override fun renderTypeParameter(node: DocumentationNode): String {
        val constraints = node.details(Kind.UpperBound)
        return if (constraints.none())
            node.name
        else {
            node.name + " : " + constraints.map { renderType(node) }.join()
        }
    }

    override fun renderParameter(node: DocumentationNode): String {
        return node.name + ": " + renderType(node.detail(Kind.Type))
    }

    override fun renderTypeParametersForNode(node: DocumentationNode): String {
        return StringBuilder {
            val typeParameters = node.details(Kind.TypeParameter)
            if (typeParameters.any()) {
                append("<")
                append(typeParameters.map { renderTypeParameter(it) }.join())
                append("> ")
            }
        }.toString()
    }

    override fun renderClass(node: DocumentationNode): String {
        return StringBuilder {
            when (node.kind) {
                Kind.Class -> append("class ")
                Kind.Trait -> append("trait ")
                Kind.Object -> append("object ")
                else -> throw IllegalArgumentException("Node $node is not a class-like object")
            }

            append(node.name)
            append(renderTypeParametersForNode(node))

            append("(")
            append(node.details(Kind.Parameter).map { renderParameter(it) }.join())
            append(")")
        }.toString()
    }

    override fun renderFunction(node: DocumentationNode): String {
        return StringBuilder {
            when (node.kind) {
                Kind.Constructor -> append("init")
                Kind.Function -> append("fun ")
                else -> throw IllegalArgumentException("Node $node is not a function-like object")
            }
            append(renderTypeParametersForNode(node))
            val receiver = node.details(Kind.Receiver).firstOrNull() // TODO: replace with singleOrNull when fixed
            if (receiver != null) {
                append(renderType(receiver.detail(Kind.Type)))
                append(".")
            }

            if (node.kind != Kind.Constructor)
                append(node.name)

            append("(")
            append(node.details(Kind.Parameter).map { renderParameter(it) }.join())
            append(")")
            if (node.kind != Kind.Constructor) {
                append(": ")
                append(renderType(node.detail(Kind.Type)))
            }
        }.toString()
    }

    override fun renderProperty(node: DocumentationNode): String {
        return StringBuilder {
            when (node.kind) {
                Kind.Property -> append("val ")
                else -> throw IllegalArgumentException("Node $node is not a property")
            }
            append(renderTypeParametersForNode(node))
            val receiver = node.details(Kind.Receiver).firstOrNull() // TODO: replace with singleOrNull when fixed
            if (receiver != null) {
                append(renderType(receiver.detail(Kind.Type)))
                append(".")
            }

            append(node.name)
            append(": ")
            append(renderType(node.detail(Kind.Type)))
        }.toString()
    }
}