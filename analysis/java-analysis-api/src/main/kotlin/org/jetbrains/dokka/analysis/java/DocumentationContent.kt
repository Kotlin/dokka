package org.jetbrains.dokka.analysis.java

interface DocumentationContent {
    val tag: JavadocTag

    fun resolveSiblings(): List<DocumentationContent>
}
