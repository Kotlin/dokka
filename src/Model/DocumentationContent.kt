package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.BindingContext

public class DocumentationContentSection(public val label: String, public val text: RichString) {
    override fun toString(): String {
        return "$label = $text"
    }
}

// TODO: refactor sections to map
public class DocumentationContent(public val summary: RichString,
                                  public val description: RichString,
                                  public val sections: List<DocumentationContentSection>) {

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
            return summary.toString()
        return "$summary | " + sections.joinToString()
    }

    val isEmpty: Boolean
        get() = description.isEmpty() && sections.none()

    class object {
        val Empty = DocumentationContent(RichString.empty, RichString.empty, listOf())
    }
}


fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): DocumentationContent {
    val docText = getDocumentationElements(descriptor).map { it.extractText() }.join("\n")
    val sections = docText.parseSections()
    val (summary, description) = sections.extractSummaryAndDescription()
    return DocumentationContent(summary, description, sections.drop(1))
}

fun List<DocumentationContentSection>.extractSummaryAndDescription() : Pair<RichString, RichString> {
    // TODO: rework to unify
    // if no $summary and $description is present, parse unnamed section and create specific sections
    // otherwise, create empty sections for missing

    val summary = firstOrNull { it.label == "\$summary" }
    if (summary != null) {
        val description = firstOrNull { it.label == "\$description" }
        return Pair(summary.text, description?.text ?: RichString.empty)
    }

    val description = firstOrNull { it.label == "\$description" }
    if (description != null) {
        return Pair(RichString.empty, description.text)
    }

    val default = firstOrNull { it.label == "" }?.text
    if (default == null)
        return Pair(RichString.empty, RichString.empty)

    return default.splitBy("\n")
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
        c == '$' -> {
            for (end in index + 1..length - 1) {
                if (Character.isWhitespace(get(end))) {
                    return substring(index, end) to end
                }
            }
            return substring(index, length) to length
        }
        c == '{' -> {
            val end = indexOf('}', index + 1)
            return substring(index + 1, end) to end + 1
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
            if (index != -1 && index < length() && get(index) == ':') {
                // section starts, add previous section
                val currentContent = substring(currentSectionStart, currentIndex).trim()
                val section = DocumentationContentSection(currentLabel, currentContent.toRichString())
                sections.add(section)

                currentLabel = label
                currentIndex = index + 1
                currentSectionStart = currentIndex
                continue
            }
        }
        currentIndex++

    }

    val currentContent = substring(currentSectionStart, currentIndex).trim()
    val section = DocumentationContentSection(currentLabel, currentContent.toRichString())
    sections.add(section)
    return sections
}

fun String.toRichString() : RichString {
    val content = RichString()
    content.addSlice(this, NormalStyle)
    return content
}