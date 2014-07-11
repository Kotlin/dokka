package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.scopes.*
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor


fun DocumentationNode.resolve(): DocumentationNode {
    return this
}

fun DocumentationNode.resolve(scope: JetScope): DocumentationNode {
    for (detail in details) {
        val symbol = when (detail.kind) {
            DocumentationNodeKind.Receiver -> (scope.getContainingDeclaration() as FunctionDescriptor).getReceiverParameter()
            DocumentationNodeKind.Parameter -> scope.getLocalVariable(Name.guess(detail.name))
            DocumentationNodeKind.Function -> scope.getFunctions(Name.guess(detail.name)).single()
            DocumentationNodeKind.Property -> scope.getProperties(Name.guess(detail.name)).single()
            DocumentationNodeKind.TypeParameter -> scope.getClassifier(Name.guess(detail.name))
            else -> scope.getClassifier(Name.guess(detail.name))
        }

        if (symbol == null)
            throw IllegalStateException("Cannot resolve node $this detail $detail")
    }
    return this
}