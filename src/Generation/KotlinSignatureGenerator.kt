package org.jetbrains.dokka

class KotlinSignatureGenerator : SignatureGenerator {
    override fun getFunctionSignature(node: DocumentationNode): String {
        return node.name
    }
}