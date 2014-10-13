package org.jetbrains.dokka

import kotlin.properties.Delegates

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
public class ContentCode() : ContentBlock()
public class ContentBlockCode() : ContentBlock()
public class ContentNodeLink(val node : DocumentationNode) : ContentBlock()
public class ContentExternalLink(val href : String) : ContentBlock()
public class ContentList() : ContentBlock()
public class ContentSection(public val label: String) : ContentBlock()

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
    public val sections: Map<String, ContentSection> by Delegates.lazy {
        val map = linkedMapOf<String, ContentSection>()
        for (child in children) {
            if (child is ContentSection)
                map.put(child.label, child)
        }

        if ("\$summary" !in map && "\$description" !in map) {
            // no explicit summary and description, convert anonymous section
            val anonymous = map[""]
            if (anonymous != null) {
                map.remove("")
                val summary = ContentSection("\$summary")
                val description = ContentSection("\$description")

                val summaryNodes = anonymous.children.take(1)
                val descriptionNodes = anonymous.children.drop(1)

                if (summaryNodes.any()) {
                    summary.children.addAll(summaryNodes)
                    map.put("\$summary", summary)
                }

                if (descriptionNodes.any()) {
                    description.children.addAll(descriptionNodes)
                    map.put("\$description", description)
                }
            }
        }
        map
    }

    public val summary: ContentNode get() = sections["\$summary"] ?: ContentNode.empty
    public val description: ContentNode get() = sections["\$description"] ?: ContentNode.empty

    override fun equals(other: Any?): Boolean {
        if (other !is Content)
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
        get() = sections.none()

    class object {
        val Empty = Content()
    }
}
