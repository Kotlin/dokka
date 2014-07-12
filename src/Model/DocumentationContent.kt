package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.BindingContext

class DocumentationContentSection(val label: String, val text: String) {
    override fun toString(): String {
        return "$label = $text"
    }
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
        if (sections.isEmpty())
            return summary
        return "$summary | " + sections.joinToString()
    }

    class object {
        val Empty = DocumentationContent("", listOf())
    }
}


fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): DocumentationContent {
    val docText = getDocumentationElements(descriptor).map { it.extractText() }.join("\n")

    val sections = docText.parseSections()

    return DocumentationContent(sections.first().text, sections.drop(1))
}

fun String.parseLabel(index: Int): Pair<String, Int> {
    val c = get(index)
    when {
        Character.isJavaIdentifierStart(c) -> {
            for (end in index + 1..length - 1) {
                if (!Character.isJavaIdentifierPart(get(end))) {
                    return substring(index, end) to end
                }
            }
            return substring(index, length) to length
        }
        c == '{' -> {
            val end = indexOf('}', index + 1)
            return substring(index + 1, end) to end + 2
        }
    }
    return "" to -1
}

fun String.parseSections(): List<DocumentationContentSection> {
    val sections = arrayListOf<DocumentationContentSection>()
    var currentLabel = ""
    var currentSectionStart = 0
    var currentIndex = 0

    while (currentIndex < length) {
        if (get(currentIndex) == '$') {
            val (label, index) = parseLabel(currentIndex + 1)
            if (index != -1) {
                // section starts, add previous section
                sections.add(DocumentationContentSection(currentLabel, substring(currentSectionStart, currentIndex).trim()))

                currentLabel = label
                currentIndex = index
                currentSectionStart = currentIndex
                continue
            }
        }
        currentIndex++

    }

    sections.add(DocumentationContentSection(currentLabel, substring(currentSectionStart, currentIndex).trim()))
    return sections
}