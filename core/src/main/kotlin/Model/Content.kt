package org.jetbrains.dokka

interface ContentNode {
    val textLength: Int
}

object ContentEmpty : ContentNode {
    override val textLength: Int get() = 0
}

open class ContentBlock() : ContentNode {
    open val children = arrayListOf<ContentNode>()

    fun append(node: ContentNode)  {
        children.add(node)
    }

    fun isEmpty() = children.isEmpty()

    override fun equals(other: Any?): Boolean =
        other is ContentBlock && javaClass == other.javaClass && children == other.children

    override fun hashCode(): Int =
        children.hashCode()

    override val textLength: Int
        get() = children.sumBy { it.textLength }
}

class NodeRenderContent(
    val node: DocumentationNode,
    val mode: LanguageService.RenderMode
): ContentNode {
    override val textLength: Int
        get() = 0 //TODO: Clarify?
}

class LazyContentBlock(private val fillChildren: () -> List<ContentNode>) : ContentBlock() {
    private var computed = false
    override val children: ArrayList<ContentNode>
        get() {
            if (!computed) {
                computed = true
                children.addAll(fillChildren())
            }
            return super.children
        }

    override fun equals(other: Any?): Boolean {
        return other is LazyContentBlock && other.fillChildren == fillChildren && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode() + 31 * fillChildren.hashCode()
    }
}

enum class IdentifierKind {
    TypeName,
    ParameterName,
    AnnotationName,
    SummarizedTypeName,
    Other
}

data class ContentText(val text: String) : ContentNode {
    override val textLength: Int
        get() = text.length
}

data class ContentKeyword(val text: String) : ContentNode {
    override val textLength: Int
        get() = text.length
}

data class ContentIdentifier(val text: String,
                             val kind: IdentifierKind = IdentifierKind.Other,
                             val signature: String? = null) : ContentNode {
    override val textLength: Int
        get() = text.length
}

data class ContentSymbol(val text: String) : ContentNode {
    override val textLength: Int
        get() = text.length
}

data class ContentEntity(val text: String) : ContentNode {
    override val textLength: Int
        get() = text.length
}

object ContentNonBreakingSpace: ContentNode {
    override val textLength: Int
        get() = 1
}

object ContentSoftLineBreak: ContentNode {
    override val textLength: Int
        get() = 0
}

object ContentIndentedSoftLineBreak: ContentNode {
    override val textLength: Int
        get() = 0
}

class ContentParagraph() : ContentBlock()
class ContentEmphasis() : ContentBlock()
class ContentStrong() : ContentBlock()
class ContentStrikethrough() : ContentBlock()
class ContentCode() : ContentBlock()
open class ContentBlockCode(val language: String = "") : ContentBlock()
class ContentBlockSampleCode(language: String = "kotlin", val importsBlock: ContentBlockCode = ContentBlockCode(language)) : ContentBlockCode(language)

abstract class ContentNodeLink() : ContentBlock() {
    abstract val node: DocumentationNode?
    abstract val text: String?
}

object ContentHardLineBreak : ContentNode {
    override val textLength: Int
        get() = 0
}

class ContentNodeDirectLink(override val node: DocumentationNode): ContentNodeLink() {
    override fun equals(other: Any?): Boolean =
            super.equals(other) && other is ContentNodeDirectLink && node.name == other.node.name

    override fun hashCode(): Int =
            children.hashCode() * 31 + node.name.hashCode()

    override val text: String? = null
}

class ContentNodeLazyLink(val linkText: String, val lazyNode: () -> DocumentationNode?): ContentNodeLink() {
    override val node: DocumentationNode? get() = lazyNode()

    override fun equals(other: Any?): Boolean =
            super.equals(other) && other is ContentNodeLazyLink && linkText == other.linkText

    override fun hashCode(): Int =
            children.hashCode() * 31 + linkText.hashCode()

    override val text: String? = linkText
}

class ContentExternalLink(val href : String) : ContentBlock() {
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is ContentExternalLink && href == other.href

    override fun hashCode(): Int =
        children.hashCode() * 31 + href.hashCode()
}

data class ContentBookmark(val name: String): ContentBlock()
data class ContentLocalLink(val href: String) : ContentBlock()

class ContentUnorderedList() : ContentBlock()
class ContentOrderedList() : ContentBlock()
class ContentListItem() : ContentBlock()

class ContentHeading(val level: Int) : ContentBlock()

class ContentSection(val tag: String, val subjectName: String?) : ContentBlock() {
    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is ContentSection && tag == other.tag && subjectName == other.subjectName

    override fun hashCode(): Int =
        children.hashCode() * 31 * 31 + tag.hashCode() * 31 + (subjectName?.hashCode() ?: 0)
}

object ContentTags {
    const val Description = "Description"
    const val SeeAlso = "See Also"
    const val Return = "Return"
    const val Exceptions = "Exceptions"
}

fun content(body: ContentBlock.() -> Unit): ContentBlock {
    val block = ContentBlock()
    block.body()
    return block
}

fun ContentBlock.text(value: String) = append(ContentText(value))
fun ContentBlock.keyword(value: String) = append(ContentKeyword(value))
fun ContentBlock.symbol(value: String) = append(ContentSymbol(value))

fun ContentBlock.identifier(value: String, kind: IdentifierKind = IdentifierKind.Other, signature: String? = null) {
    append(ContentIdentifier(value, kind, signature))
}

fun ContentBlock.nbsp() = append(ContentNonBreakingSpace)
fun ContentBlock.softLineBreak() = append(ContentSoftLineBreak)
fun ContentBlock.indentedSoftLineBreak() = append(ContentIndentedSoftLineBreak)
fun ContentBlock.hardLineBreak() = append(ContentHardLineBreak)

fun ContentBlock.strong(body: ContentBlock.() -> Unit) {
    val strong = ContentStrong()
    strong.body()
    append(strong)
}

fun ContentBlock.code(body: ContentBlock.() -> Unit) {
    val code = ContentCode()
    code.body()
    append(code)
}

fun ContentBlock.link(to: DocumentationNode, body: ContentBlock.() -> Unit) {
    val block = if (to.kind == NodeKind.ExternalLink)
        ContentExternalLink(to.name)
    else
        ContentNodeDirectLink(to)

    block.body()
    append(block)
}

open class Content(): ContentBlock() {
    open val sections: List<ContentSection> get() = emptyList()
    open val summary: ContentNode get() = ContentEmpty
    open val description: ContentNode get() = ContentEmpty

    fun findSectionByTag(tag: String): ContentSection? =
            sections.firstOrNull { tag.equals(it.tag, ignoreCase = true) }

    companion object {
        val Empty = object: Content() {
            override fun toString(): String {
                return "EMPTY_CONTENT"
            }
        }

        fun of(vararg child: ContentNode): Content {
            val result = MutableContent()
            child.forEach { result.append(it) }
            return result
        }
    }
}

open class MutableContent() : Content() {
    private val sectionList = arrayListOf<ContentSection>()
    override val sections: List<ContentSection>
        get() = sectionList

    fun addSection(tag: String?, subjectName: String?): ContentSection {
        val section = ContentSection(tag ?: "", subjectName)
        sectionList.add(section)
        return section
    }

    override val summary: ContentNode get() = children.firstOrNull() ?: ContentEmpty

    override val description: ContentNode by lazy {
        val descriptionNodes = children.drop(1)
        if (descriptionNodes.isEmpty()) {
            ContentEmpty
        } else {
            val result = ContentSection(ContentTags.Description, null)
            result.children.addAll(descriptionNodes)
            result
        }
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
        return (listOf(summary, description) + sections).joinToString()
    }
}

fun javadocSectionDisplayName(sectionName: String?): String? =
        when(sectionName) {
            "param" -> "Parameters"
            "throws", "exception" -> ContentTags.Exceptions
            else -> sectionName?.capitalize()
        }
