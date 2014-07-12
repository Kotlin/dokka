package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.lang.resolve.BindingContext

fun BindingContext.checkResolveChildren(node : DocumentationNode) {
    if (node.kind != DocumentationNodeKind.Module && node.kind != DocumentationNodeKind.Package) {
        // TODO: we don't resolve packages and modules for now

        val parentScope = getResolutionScope(node.descriptor)
        for (item in node.details + node.members) {
            val symbolName = item.name
            val symbol: DeclarationDescriptor? = when (item.kind) {
                DocumentationNodeKind.Receiver -> (parentScope.getContainingDeclaration() as FunctionDescriptor).getReceiverParameter()
                DocumentationNodeKind.Parameter -> parentScope.getLocalVariable(Name.guess(symbolName))
                DocumentationNodeKind.Function -> parentScope.getFunctions(Name.guess(symbolName)).firstOrNull()
                DocumentationNodeKind.Property -> parentScope.getProperties(Name.guess(symbolName)).firstOrNull()
                DocumentationNodeKind.Constructor -> parentScope.getFunctions(Name.guess(symbolName)).firstOrNull()
                else -> parentScope.getClassifier(Name.guess(symbolName))
            }

            if (symbol == null)
                println("WARNING: Cannot resolve $item in ${path(node)}")
        }
    }

    for (reference in node.allReferences().filterNot { it.kind == DocumentationReferenceKind.Owner }) {
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