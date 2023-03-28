package org.jetbrains.dokka.analysis.java

import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
interface DocumentationContent {
    val tag: JavadocTag

    fun resolveSiblings(): List<DocumentationContent>
}
