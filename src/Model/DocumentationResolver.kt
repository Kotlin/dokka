package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.scopes.*
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.lang.descriptors.*

fun DocumentationNode.resolve(): DocumentationNode {
    for (detail in details) {
        val symbol: DeclarationDescriptor? = when (detail.kind) {
            DocumentationNodeKind.Receiver -> (scope.getContainingDeclaration() as FunctionDescriptor).getReceiverParameter()
            DocumentationNodeKind.Parameter -> scope.getLocalVariable(Name.guess(detail.name))
            DocumentationNodeKind.Function -> scope.getFunctions(Name.guess(detail.name)).firstOrNull()
            DocumentationNodeKind.Property -> scope.getProperties(Name.guess(detail.name)).firstOrNull()
            DocumentationNodeKind.TypeParameter -> scope.getClassifier(Name.guess(detail.name))
            else -> scope.getClassifier(Name.guess(detail.name))
        }

        if (symbol == null)
            throw IllegalStateException("Cannot resolve node $this detail $detail")
    }
    return this
}

