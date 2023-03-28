package org.jetbrains.dokka.analysis.java.doctag

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.DocumentationContent

@InternalDokkaApi
interface InheritDocTagContentProvider {
    fun canConvert(content: DocumentationContent): Boolean
    fun convertToHtml(content: DocumentationContent, docTagParserContext: DocTagParserContext): String
}
