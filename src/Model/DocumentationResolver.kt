package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.lang.descriptors.*

fun DocumentationNode.resolve() {
    for (item in details + members) {
        val symbolName = item.name
        val symbol: DeclarationDescriptor? = when (item.kind) {
            DocumentationNodeKind.Receiver -> (scope.getContainingDeclaration() as FunctionDescriptor).getReceiverParameter()
            DocumentationNodeKind.Parameter -> scope.getLocalVariable(Name.guess(symbolName))
            DocumentationNodeKind.Function -> scope.getFunctions(Name.guess(symbolName)).firstOrNull()
            DocumentationNodeKind.Property -> scope.getProperties(Name.guess(symbolName)).firstOrNull()
            DocumentationNodeKind.Constructor -> scope.getFunctions(Name.guess(symbolName)).firstOrNull()

            DocumentationNodeKind.Package -> {
                // TODO: do not resolve constructors and packages for now
                item.scope.getContainingDeclaration()
            }
            else -> scope.getClassifier(Name.guess(symbolName))
        }

        if (symbol == null)
            throw IllegalStateException("Cannot resolve $item in $this")
    }

    for (reference in allReferences().filterNot { it.kind == DocumentationReferenceKind.Owner }) {
        reference.to.resolve()
    }
}

fun DocumentationModel.resolveAll() {
    resolve()
}