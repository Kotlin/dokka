package org.jetbrains.dokka.analysis.java

import org.jetbrains.dokka.InternalDokkaApi

/**
 * MUST override equals and hashcode
 */
@InternalDokkaApi
interface DocComment {
    fun hasTag(tag: JavadocTag): Boolean

    fun resolveTag(tag: JavadocTag): List<DocumentationContent>
}
