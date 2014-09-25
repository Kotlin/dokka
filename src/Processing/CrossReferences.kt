package org.jetbrains.dokka

public fun DocumentationNode.buildCrossReferences() {
    for (member in members) {
        member.buildCrossReferences()
        member.details(DocumentationNode.Kind.Receiver).forEach { detail ->


        }
    }
}

