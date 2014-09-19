package org.jetbrains.dokka

import org.jetbrains.dokka.DocumentationNode.*

class KotlinLanguageService : LanguageService {
    override fun render(node: DocumentationNode): String {
        return when (node.kind) {
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
            else -> "${node.kind}: ${node.name}"
        }
    }

    override fun renderName(node: DocumentationNode): String {
        return when (node.kind) {
            Kind.Constructor -> node.owner!!.name
            else -> node.name
        }
    }

    override fun renderPackage(node: DocumentationNode): String {
        return "package ${node.name}"
    }

    override fun renderType(node: DocumentationNode): String {
        val typeArguments = node.details(Kind.Type)
        val renders = typeArguments.map { renderType(it) }

        if (node.name == "Function${typeArguments.count() - 1}") {
            // lambda
            return "(${renders.take(renders.size - 1).join()}) -> ${renders.last()}"
        }
        if (node.name == "ExtensionFunction${typeArguments.count() - 2}") {
            // extension lambda
            return "${renders.first()}.(${renders.drop(1).take(renders.size - 2).join()}) -> ${renders.last()}"
        }
        if (typeArguments.none())
            return node.name
        return "${node.name}<${renders.join()}>"
    }

    override fun renderModifier(node: DocumentationNode): String {
        return when (node.name) {
            "final" -> ""
            "internal" -> ""
            else -> node.name
        }
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

    override fun renderModifiersForNode(node: DocumentationNode): String {
        val modifiers = node.details(Kind.Modifier).map { renderModifier(it) }.filter { it != "" }
        if (modifiers.none())
            return ""
        return modifiers.join(" ", postfix = " ")
    }

    override fun renderClass(node: DocumentationNode): String {
        return StringBuilder {
            append(renderModifiersForNode(node))
            when (node.kind) {
                Kind.Class -> append("class ")
                Kind.Interface -> append("trait ")
                Kind.Enum -> append("enum class ")
                Kind.EnumItem -> append("enum val ")
                Kind.Object -> append("object ")
                else -> throw IllegalArgumentException("Node $node is not a class-like object")
            }

            append(node.name)
            append(renderTypeParametersForNode(node))

            /*
                        val constructors = node.members(Kind.Constructor)
                        if (constructors.count() == 1) {
                            append("(")
                            append(constructors[0].details(Kind.Parameter).map { renderParameter(it) }.join())
                            append(")")
                        }
            */
        }.toString()
    }

    override fun renderFunction(node: DocumentationNode): String {
        return StringBuilder {
            append(renderModifiersForNode(node))
            when (node.kind) {
                Kind.Constructor -> append(node.owner!!.name)
                Kind.Function -> append("fun ")
                else -> throw IllegalArgumentException("Node $node is not a function-like object")
            }
            append(renderTypeParametersForNode(node))
            val receiver = node.details(Kind.Receiver).singleOrNull()
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
            append(renderModifiersForNode(node))
            when (node.kind) {
                Kind.Property -> append("val ")
                else -> throw IllegalArgumentException("Node $node is not a property")
            }
            append(renderTypeParametersForNode(node))
            val receiver = node.details(Kind.Receiver).singleOrNull()
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