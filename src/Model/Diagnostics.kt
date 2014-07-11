package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.*

fun DocumentationNode.checkResolve() {
    val parentScope = scope

    for (item in details + members) {
        val symbolName = item.name
        val symbol: DeclarationDescriptor? = when (item.kind) {
            DocumentationNodeKind.Receiver -> (parentScope.getContainingDeclaration() as FunctionDescriptor).getReceiverParameter()
            DocumentationNodeKind.Parameter -> parentScope.getLocalVariable(Name.guess(symbolName))
            DocumentationNodeKind.Function -> parentScope.getFunctions(Name.guess(symbolName)).firstOrNull()
            DocumentationNodeKind.Property -> parentScope.getProperties(Name.guess(symbolName)).firstOrNull()
            DocumentationNodeKind.Constructor -> parentScope.getFunctions(Name.guess(symbolName)).firstOrNull()

            DocumentationNodeKind.Package -> {
                // TODO: do not resolve constructors and packages for now
                item.scope.getContainingDeclaration()
            }
            else -> parentScope.getClassifier(Name.guess(symbolName))
        }

        if (symbol == null)
            println("WARNING: Cannot resolve $item in ${path(this)}")
    }

    for (reference in allReferences().filterNot { it.kind == DocumentationReferenceKind.Owner }) {
        reference.to.checkResolve()
    }
}

fun path(node: DocumentationNode): String {
    val owner = node.owner
    if (owner != null)
        return "$node in ${path(owner)}"
    else
        return "$node"
}