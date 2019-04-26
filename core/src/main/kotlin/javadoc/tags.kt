package org.jetbrains.dokka.javadoc

import com.sun.javadoc.*
import org.jetbrains.dokka.*
import java.util.*

class TagImpl(val holder: Doc, val name: String, val text: String): Tag {
    override fun text(): String? = text

    override fun holder(): Doc = holder
    override fun firstSentenceTags(): Array<out Tag>? = arrayOf()
    override fun inlineTags(): Array<out Tag>?  = arrayOf()

    override fun name(): String = name
    override fun kind(): String = name

    override fun position(): SourcePosition = holder.position()
}

class TextTag(val holder: Doc, val content: ContentText) : Tag {
    val plainText: String
        get() = content.text

    override fun name(): String = "Text"
    override fun kind(): String = name()
    override fun text(): String? = plainText
    override fun inlineTags(): Array<out Tag> = arrayOf(this)
    override fun holder(): Doc = holder
    override fun firstSentenceTags(): Array<out Tag> = arrayOf(this)
    override fun position(): SourcePosition = holder.position()
}

abstract class SeeTagAdapter(val holder: Doc, val content: ContentNodeLink) : SeeTag {
    override fun position(): SourcePosition? = holder.position()
    override fun name(): String = "@see"
    override fun kind(): String = "@see"
    override fun holder(): Doc = holder

    override fun text(): String? = content.node?.name ?: "(?)"
}

class SeeExternalLinkTagAdapter(val holder: Doc, val link: ContentExternalLink) : SeeTag {
    override fun position(): SourcePosition = holder.position()
    override fun text(): String = label()
    override fun inlineTags(): Array<out Tag> = emptyArray() // TODO

    override fun label(): String {
        val label = link.asText() ?: link.href
        return "<a href=\"${link.href}\">$label</a>"
    }

    override fun referencedPackage(): PackageDoc? = null
    override fun referencedClass(): ClassDoc? = null
    override fun referencedMemberName(): String? = null
    override fun referencedClassName(): String? = null
    override fun referencedMember(): MemberDoc? = null
    override fun holder(): Doc = holder
    override fun firstSentenceTags(): Array<out Tag> = inlineTags()
    override fun name(): String = "@link"
    override fun kind(): String = "@see"
}

fun ContentBlock.asText(): String? {
    val contentText = children.singleOrNull() as? ContentText
    return contentText?.text
}

class SeeMethodTagAdapter(holder: Doc, val method: MethodAdapter, content: ContentNodeLink) : SeeTagAdapter(holder, content) {
    override fun referencedMember(): MemberDoc = method
    override fun referencedMemberName(): String = method.name()
    override fun referencedPackage(): PackageDoc? = null
    override fun referencedClass(): ClassDoc? = method.containingClass()
    override fun referencedClassName(): String = method.containingClass()?.name() ?: ""
    override fun label(): String = "${method.containingClass()?.name()}.${method.name()}"

    override fun inlineTags(): Array<out Tag> = emptyArray() // TODO
    override fun firstSentenceTags(): Array<out Tag> = inlineTags() // TODO
}

class SeeClassTagAdapter(holder: Doc, val clazz: ClassDocumentationNodeAdapter, content: ContentNodeLink) : SeeTagAdapter(holder, content) {
    override fun referencedMember(): MemberDoc? = null
    override fun referencedMemberName(): String? = null
    override fun referencedPackage(): PackageDoc? = null
    override fun referencedClass(): ClassDoc = clazz
    override fun referencedClassName(): String = clazz.name()
    override fun label(): String = "${clazz.classNode.kind.name.toLowerCase()} ${clazz.name()}"

    override fun inlineTags(): Array<out Tag> = emptyArray() // TODO
    override fun firstSentenceTags(): Array<out Tag> = inlineTags() // TODO
}

class ParamTagAdapter(val module: ModuleNodeAdapter,
                      val holder: Doc,
                      val parameterName: String,
                      val typeParameter: Boolean,
                      val content: List<ContentNode>) : ParamTag {

    constructor(module: ModuleNodeAdapter, holder: Doc, parameterName: String, isTypeParameter: Boolean, content: ContentNode)
        : this(module, holder, parameterName, isTypeParameter, listOf(content)) {
    }

    override fun name(): String = "@param"
    override fun kind(): String = name()
    override fun holder(): Doc = holder
    override fun position(): SourcePosition? = holder.position()

    override fun text(): String = "@param $parameterName ${parameterComment()}" // Seems has no effect, so used for debug
    override fun inlineTags(): Array<out Tag> = buildInlineTags(module, holder, content).toTypedArray()
    override fun firstSentenceTags(): Array<out Tag> = arrayOf(TextTag(holder, ContentText(text())))

    override fun isTypeParameter(): Boolean = typeParameter
    override fun parameterComment(): String = content.toString() // TODO
    override fun parameterName(): String = parameterName
}


class ThrowsTagAdapter(val holder: Doc, val type: ClassDocumentationNodeAdapter, val content: List<ContentNode>) : ThrowsTag {
    override fun name(): String = "@throws"
    override fun kind(): String = name()
    override fun holder(): Doc = holder
    override fun position(): SourcePosition? = holder.position()

    override fun text(): String = "${name()} ${exceptionName()} ${exceptionComment()}"
    override fun inlineTags(): Array<out Tag> = buildInlineTags(type.module, holder, content).toTypedArray()
    override fun firstSentenceTags(): Array<out Tag> = emptyArray()

    override fun exceptionComment(): String = content.toString()
    override fun exceptionType(): Type = type
    override fun exception(): ClassDoc = type
    override fun exceptionName(): String = type.qualifiedTypeName()
}

class ReturnTagAdapter(val module: ModuleNodeAdapter, val holder: Doc, val content: List<ContentNode>) : Tag {
    override fun name(): String = "@return"
    override fun kind() = name()
    override fun holder() = holder
    override fun position(): SourcePosition? = holder.position()

    override fun text(): String = "@return $content" // Seems has no effect, so used for debug
    override fun inlineTags(): Array<Tag> = buildInlineTags(module, holder, content).toTypedArray()
    override fun firstSentenceTags(): Array<Tag> = inlineTags()
}

fun buildInlineTags(module: ModuleNodeAdapter, holder: Doc, tags: List<ContentNode>): List<Tag> = ArrayList<Tag>().apply { tags.forEach { buildInlineTags(module, holder, it, this) } }

fun buildInlineTags(module: ModuleNodeAdapter, holder: Doc, root: ContentNode): List<Tag> = ArrayList<Tag>().apply { buildInlineTags(module, holder, root, this) }

private fun buildInlineTags(module: ModuleNodeAdapter, holder: Doc, nodes: List<ContentNode>, result: MutableList<Tag>) {
    nodes.forEach {
        buildInlineTags(module, holder, it, result)
    }
}


private fun buildInlineTags(module: ModuleNodeAdapter, holder: Doc, node: ContentNode, result: MutableList<Tag>) {
    fun surroundWith(module: ModuleNodeAdapter, holder: Doc, prefix: String, postfix: String, node: ContentBlock, result: MutableList<Tag>) {
        if (node.children.isNotEmpty()) {
            val open = TextTag(holder, ContentText(prefix))
            val close = TextTag(holder, ContentText(postfix))

            result.add(open)
            buildInlineTags(module, holder, node.children, result)

            if (result.last() === open) {
                result.removeAt(result.lastIndex)
            } else {
                result.add(close)
            }
        }
    }

    fun surroundWith(module: ModuleNodeAdapter, holder: Doc, prefix: String, postfix: String, node: ContentNode, result: MutableList<Tag>) {
        if (node !is ContentEmpty) {
            val open = TextTag(holder, ContentText(prefix))
            val close = TextTag(holder, ContentText(postfix))

            result.add(open)
            buildInlineTags(module, holder, node, result)
            if (result.last() === open) {
                result.removeAt(result.lastIndex)
            } else {
                result.add(close)
            }
        }
    }

    when (node) {
        is ContentText -> result.add(TextTag(holder, node))
        is ContentNodeLink -> {
            val target = node.node
            when (target?.kind) {
                NodeKind.Function -> result.add(SeeMethodTagAdapter(holder, MethodAdapter(module, node.node!!), node))

                in NodeKind.classLike -> result.add(SeeClassTagAdapter(holder, ClassDocumentationNodeAdapter(module, node.node!!), node))

                else -> buildInlineTags(module, holder, node.children, result)
            }
        }
        is ContentExternalLink -> result.add(SeeExternalLinkTagAdapter(holder, node))
        is ContentSpecialReference -> surroundWith(module, holder, "<aside class=\"note\">", "</aside>", node, result)
        is ContentCode -> surroundWith(module, holder, "<code>", "</code>", node, result)
        is ContentBlockCode -> surroundWith(module, holder, "<code><pre>", "</pre></code>", node, result)
        is ContentEmpty -> {}
        is ContentEmphasis -> surroundWith(module, holder, "<em>", "</em>", node, result)
        is ContentHeading -> surroundWith(module, holder, "<h${node.level}>", "</h${node.level}>", node, result)
        is ContentEntity -> result.add(TextTag(holder, ContentText(node.text))) // TODO ??
        is ContentIdentifier -> result.add(TextTag(holder, ContentText(node.text))) // TODO
        is ContentKeyword -> result.add(TextTag(holder, ContentText(node.text))) // TODO
        is ContentListItem ->  surroundWith(module, holder, "<li>", "</li>", node, result)
        is ContentOrderedList -> surroundWith(module, holder, "<ol>", "</ol>", node, result)
        is ContentUnorderedList -> surroundWith(module, holder, "<ul>", "</ul>", node, result)
        is ContentParagraph -> surroundWith(module, holder, "<p>", "</p>", node, result)

        is ContentDescriptionList -> surroundWith(module, holder, "<dl>", "</dl>", node, result)
        is ContentDescriptionTerm -> surroundWith(module, holder, "<dt>", "</dt>", node, result)
        is ContentDescriptionDefinition -> surroundWith(module, holder, "<dd>", "</dd>", node, result)

        is ContentTable -> surroundWith(module, holder, "<table>", "</table>", node, result)
        is ContentTableBody -> surroundWith(module, holder, "<tbody>", "</tbody>", node, result)
        is ContentTableRow -> surroundWith(module, holder, "<tr>", "</tr>", node, result)
        is ContentTableHeader -> surroundWith(module, holder, "<th>", "</th>", node, result)
        is ContentTableCell -> surroundWith(module, holder, "<td>", "</td>", node, result)

        is ContentSection -> surroundWith(module, holder, "<p>", "</p>", node, result) // TODO how section should be represented?
        is ContentNonBreakingSpace -> result.add(TextTag(holder, ContentText("&nbsp;")))
        is ContentStrikethrough -> surroundWith(module, holder, "<strike>", "</strike>", node, result)
        is ContentStrong -> surroundWith(module, holder, "<strong>", "</strong>", node, result)
        is ContentSymbol -> result.add(TextTag(holder, ContentText(node.text))) // TODO?
        is Content -> {
            surroundWith(module, holder, "<p>", "</p>", node.summary, result)
            surroundWith(module, holder, "<p>", "</p>", node.description, result)
        }
        is ContentBlock -> {
            surroundWith(module, holder, "", "", node, result)
        }
        is ContentHardLineBreak -> result.add(TextTag(holder, ContentText("<br/>")))

        else -> result.add(TextTag(holder, ContentText("$node")))
    }
}