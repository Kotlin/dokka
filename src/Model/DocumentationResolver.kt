package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.scopes.*


fun DocumentationNode.resolve(): DocumentationNode {
    return this
}

fun DocumentationNode.resolve(scope: JetScope): DocumentationNode {
    return this
}