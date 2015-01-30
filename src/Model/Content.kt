package org.jetbrains.dokka

public abstract class ContentNode {
    val children = arrayListOf<ContentNode>()

    class object {
        val empty = ContentEmpty
    }

    fun append(node : ContentNode)  {
        children.add(node)
    }

    fun isEmpty() = children.isEmpty()
}

public object ContentEmpty : ContentNode()
public open class ContentBlock() : ContentNode()

public class ContentText(val text: String) : ContentNode()
public class ContentKeyword(val text: String) : ContentNode()
public class ContentIdentifier(val text: String) : ContentNode()
public class ContentSymbol(val text: String) : ContentNode()

public class ContentParagraph() : ContentBlock()
public class ContentEmphasis() : ContentBlock()
public class ContentStrong() : ContentBlock()
public class ContentStrikethrough() : ContentBlock()
public class ContentCode() : ContentBlock()
public class ContentBlockCode() : ContentBlock()
public class ContentNodeLink(val node : DocumentationNode) : ContentBlock()
public class ContentExternalLink(val href : String) : ContentBlock()
public class ContentList() : ContentBlock()
public class ContentListItem() : ContentBlock()
public class ContentSection(public val label: String, public val subjectName: String?) : ContentBlock()

fun content(body: ContentNode.() -> Unit): ContentNode {
    val block = ContentBlock()
    block.body()
    return block
}

fun ContentNode.text(value: String) = append(ContentText(value))
fun ContentNode.keyword(value: String) = append(ContentKeyword(value))
fun ContentNode.symbol(value: String) = append(ContentSymbol(value))
fun ContentNode.identifier(value: String) = append(ContentIdentifier(value))

fun ContentNode.link(to: DocumentationNode, body: ContentNode.() -> Unit) {
    val block = ContentNodeLink(to)
    block.body()
    append(block)
}

public class Content() : ContentNode() {
    private val sectionList = arrayListOf<ContentSection>()
    public val sections: List<ContentSection>
        get() = sectionList

    fun addSection(name: String?, subjectName: String?): ContentSection {
        val section = ContentSection(name ?: "", subjectName)
        sectionList.add(section)
        return section
    }

    fun findSectionByName(name: String): ContentSection? =
        sections.firstOrNull { it.label == name }

    fun getSectionsWithSubjects(): Map<String, List<ContentSection>> =
        sections.filter { it.subjectName != null }.groupBy { it.label }

    public val summary: ContentNode get() = children.firstOrNull() ?: ContentEmpty

    public val description: ContentNode get() {
        val descriptionNodes = children.drop(1)
        if (descriptionNodes.isEmpty()) {
            return ContentEmpty
        }
        val result = ContentSection("\$description", null)
        result.children.addAll(descriptionNodes)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Content)
            return false
        return sections == other.sections && children == other.children
    }

    override fun hashCode(): Int {
        return sections.map { it.hashCode() }.sum()
    }

    override fun toString(): String {
        if (sections.isEmpty())
            return "<empty>"
        return sections.joinToString()
    }

    val isEmpty: Boolean
        get() = sections.none()

    class object {
        val Empty = Content()
    }
}
