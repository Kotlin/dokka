package org.jetbrains.dokka

trait SignatureGenerator {
    fun getFunctionSignature(node: DocumentationNode): String
}

