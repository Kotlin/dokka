package org.jetbrains.dokka.analysis.java

/**
 * MUST override equals and hashcode
 */
interface DocComment {
    fun hasTag(tag: JavadocTag): Boolean

    fun resolveTag(tag: JavadocTag): List<DocumentationContent>
}
