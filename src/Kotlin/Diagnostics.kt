package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.lang.resolve.BindingContext

fun DocumentationContext.checkResolveChildren(node: DocumentationNode) {
    if (node.kind != DocumentationNode.Kind.Module && node.kind != DocumentationNode.Kind.Package) {
        // TODO: we don't resolve packages and modules for now

        val parentScope = getResolutionScope(node)
        for (item in node.details + node.members) {
            val symbolName = item.name
            val symbol: DeclarationDescriptor? = when (item.kind) {
                DocumentationNode.Kind.Modifier -> continue // do not resolve modifiers, they are not names
                DocumentationNode.Kind.Receiver -> continue // what is receiver's name in platform?
                DocumentationNode.Kind.Parameter -> parentScope.getLocalVariable(Name.guess(symbolName))
                DocumentationNode.Kind.Function -> parentScope.getFunctions(Name.guess(symbolName)).firstOrNull()
                DocumentationNode.Kind.Property -> parentScope.getProperties(Name.guess(symbolName)).firstOrNull()
                DocumentationNode.Kind.Constructor -> parentScope.getFunctions(Name.guess(symbolName)).firstOrNull()
                else -> parentScope.getClassifier(Name.guess(symbolName))
            }

            if (symbol == null)
                println("WARNING: Cannot resolve $item in ${path(node)}")
        }
    }

    for (reference in node.allReferences().filterNot { it.kind == DocumentationReference.Kind.Owner }) {
        checkResolveChildren(reference.to)
    }
}

fun path(node: DocumentationNode): String {
    val owner = node.owner
    if (owner != null)
        return "$node in ${path(owner)}"
    else
        return "$node"
}