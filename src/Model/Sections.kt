package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.BindingContext

fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): DocumentationContent {
    val docText = getDocumentationElements(descriptor).map { it.extractText() }.join("\n")
    return DocumentationContent(docText, listOf())
}

class DocumentationContentSection(val label: String, val text: String) {

}

class DocumentationContent(val summary: String, val sections: List<DocumentationContentSection>) {

    override fun equals(other: Any?): Boolean {
        if (other !is DocumentationContent)
            return false
        if (summary != other.summary)
            return false
        if (sections.size != other.sections.size)
            return false
        for (index in sections.indices)
            if (sections[index] != other.sections[index])
                return false

        return true
    }

    override fun hashCode(): Int {
        return summary.hashCode() + sections.map { it.hashCode() }.sum()
    }

    override fun toString(): String {
        return "$summary | " + sections.joinToString()
    }

    class object {
        val Empty = DocumentationContent("", listOf())
    }
}

