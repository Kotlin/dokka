package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java

import org.jetbrains.dokka.analysis.java.DocumentationContent
import org.jetbrains.dokka.analysis.java.JavadocTag
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

internal data class DescriptorDocumentationContent(
    val descriptor: DeclarationDescriptor,
    val element: KDocTag,
    override val tag: JavadocTag,
) : DocumentationContent {
    override fun resolveSiblings(): List<DocumentationContent> {
        return listOf(this)
    }
}
