package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.BindingContext

public class DocumentationContentSection(public val label: String, public val text: RichString) {
    override fun toString(): String {
        return "$label = $text"
    }
}

public class DocumentationContent(public val sections: Map<String, DocumentationContentSection>) {

    public val summary: RichString get() = sections["\$summary"]?.text ?: RichString.empty
    public val description: RichString get() = sections["\$description"]?.text ?: RichString.empty

    override fun equals(other: Any?): Boolean {
        if (other !is DocumentationContent)
            return false
        if (sections.size != other.sections.size)
            return false
        for (keys in sections.keySet())
            if (sections[keys] != other.sections[keys])
                return false

        return true
    }

    override fun hashCode(): Int {
        return sections.map { it.hashCode() }.sum()
    }

    override fun toString(): String {
        if (sections.isEmpty())
            return "<empty>"
        return sections.values().joinToString()
    }

    val isEmpty: Boolean
        get() = description.isEmpty() && sections.none()

    class object {
        val Empty = DocumentationContent(mapOf())
    }
}


fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): DocumentationContent {
    val docText = getDocumentationElements(descriptor).map { it.extractText() }.join("\n")
    val sections = docText.parseSections()
    sections.createSummaryAndDescription()
    return DocumentationContent(sections)
}

fun MutableMap<String, DocumentationContentSection>.createSummaryAndDescription() {

    val summary = get("\$summary")
    val description = get("\$description")
    if (summary != null && description == null) {
        return
    }

    if (summary == null && description != null) {
        return
    }

    val unnamed = get("")
    if (unnamed == null) {
        return
    }

    val split = unnamed.text.splitBy("\n")
    remove("")
    if (!split.first.isEmpty())
        put("\$summary", DocumentationContentSection("\$summary", split.first))
    if (!split.second.isEmpty())
        put("\$description", DocumentationContentSection("\$description", split.second))
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

fun String.parseSections(): MutableMap<String, DocumentationContentSection> {
    val sections = hashMapOf<String, DocumentationContentSection>()
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
                sections.put(section.label, section)

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
    sections.put(section.label, section)
    return sections
}

fun String.toRichString() : RichString {
    val content = RichString()
    for(index in indices) {
        val ch = get(index)
        when {
            ch == '\\' -> continue
            ch == '*' && index < length-1 && !get(index + 1).isWhitespace() -> ch
        }
    }

    content.addSlice(this, NormalStyle)
    return content
}