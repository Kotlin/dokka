/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public sealed interface KdDocumented {
    public val documentation: List<KdDocumentationNode>
}

// similar to Dokka's `ContentNode`
// parsed markdown representation: text, links, etc
@Serializable
public sealed class KdDocumentationNode {
    @SerialName("text")
    @Serializable
    public data class Text(
        public val value: String,
        public val styles: Set<Style> = emptySet()
    ) : KdDocumentationNode() {
        public enum class Style {
            Italic, Strong, Strikethrough
        }
    }

    @SerialName("html")
    @Serializable
    public data class Html(public val value: String) : KdDocumentationNode()

    // TODO: sample/snippet is really similar to code block, but also allows to add highlighting or links into it
    //  + it's possible to run it
    @SerialName("codeBlock")
    @Serializable
    public data class CodeBlock(
        public val lines: List<String>,
        public val language: String? = null
    ) : KdDocumentationNode()

    @SerialName("codeInline")
    @Serializable
    public data class CodeInline(
        public val text: String,
        public val language: String? = null,
        public val styles: Set<Text.Style> = emptySet()
    ) : KdDocumentationNode()

    @SerialName("link")
    @Serializable
    public data class Link(
        val label: List<KdDocumentationNode>,
        val reference: KdLinkReference,
        val styles: Set<Text.Style> = emptySet()
    ) : KdDocumentationNode()

    @SerialName("externalLink")
    @Serializable
    public data class ExternalLink(
        val label: List<KdDocumentationNode>,
        val url: String,
        val styles: Set<Text.Style> = emptySet()
    ) : KdDocumentationNode()

    @SerialName("paragraph")
    @Serializable
    public data class Paragraph(public val children: List<KdDocumentationNode>) : KdDocumentationNode()

    @SerialName("blockQuote")
    @Serializable
    public data class BlockQuote(public val children: List<KdDocumentationNode>) : KdDocumentationNode()

    @SerialName("header")
    @Serializable
    public data class Header(
        public val level: Int,
        public val children: List<KdDocumentationNode>
    ) : KdDocumentationNode()

    @SerialName("table")
    @Serializable
    public data class Table(
        public val headers: List</*Paragraph*/ KdDocumentationNode>?,
        public val rows: List<List</*Paragraph*/ KdDocumentationNode>>
    ) : KdDocumentationNode()

    @SerialName("bulletList")
    @Serializable
    public data class BulletList(public val items: List</*Paragraph*/ KdDocumentationNode>) : KdDocumentationNode()

    @SerialName("orderedList")
    @Serializable
    public data class OrderedList(
        public val startIndex: Int,
        public val items: List</*Paragraph*/ KdDocumentationNode>
    ) : KdDocumentationNode()

    // TODO: somehow support this
    @SerialName("descriptionList")
    @Serializable
    public data class DescriptionList(public val items: List<Item>) : KdDocumentationNode() {
        @Serializable
        public data class Item(
            public val term: List<KdDocumentationNode>,
            public val description: List<KdDocumentationNode>
        )
    }

    // -------------------------

    // tags:
    // - ref + optional docs (@param, @throws)
    // - docs (@return, @since, @author)
    // - @see - TBD - multiple syntaxes
    // - ref (@sample)

    // KDoc tags (add JavaDoc tags)
    // AUTHOR - PLAIN TAG
    // SINCE - PLAIN TAG
    // SEE - REFERENCE TAG (TBD)

    // those tags will not map to `Tag` at all

    // RETURN - PLAIN TAG -> transforms into docs for return value
    // THROWS/EXCEPTION - REFERENCE TAG -> transforms into docs for throw
    // RECEIVER - PLAIN TAG -> transforms into docs for receiver
    // PARAM - REFERENCE TAG -> transforms into docs for parameter
    // CONSTRUCTOR - CONTEXT TAG -> transforms into docs for constructor
    // PROPERTY - CONTEXT TAG -> transforms into docs for property
    // SAMPLE - SPECIAL TAG -> TODO
    // SUPPRESS - SPECIAL TAG -> skips declaration

    // TODO: may be reference should also sometimes allow external links
    // TODO: may be another name
    @SerialName("tag")
    @Serializable
    public data class Tag(
        val name: String,
        val reference: KdLinkReference? = null, // TODO: what type should be here?
        val children: List<KdDocumentationNode> = emptyList(),
    ) : KdDocumentationNode()

    /**
     * javadoc tags
     *
     * block tags:
     * - author name
     * - deprecated text
     * - exception/throws name
     * - hidden = suppress
     * - param = param
     * - provides = for service loaders
     * - uses = for service loaders
     * - return = return
     * - see = like Kotlin see, but more
     * - serial = for serialization
     * - serialData = for serialization
     * - serialField = for serialization
     * - since = since
     * - spec URL title = (JDK 20) similar to `see` but specific?
     * - version xxx =
     *
     * inline tags:
     * - code = `code`
     * - docRoot = ref to root for images/etc
     * - index "phrase" description =
     * - inheritDoc [supertype] = inlines inherited documentation from optional supertype
     * - link = `[link]`/`[name][link]`
     * - linkplain = same as `link`, but displays as plain text
     * - literal text = "raw strings"
     * - return = shortcut for main doc + block tag: `Returns xxx`
     * - snippet = you know, snippets
     * - summary text = used in place of `first sentence` for summary
     * - systemProperty name = ref to system property?
     * - value = inlines value of constant
     */

}

// TODO: naming, values
// TODO: could be serialized into a single `string`
@Serializable
public sealed class KdLinkReference {
    @SerialName("receiver")
    @Serializable
    public data object Receiver : KdLinkReference()

    @SerialName("valueParameter")
    @Serializable
    public data class ValueParameter(val index: Int) : KdLinkReference()

    @SerialName("contextParameter")
    @Serializable
    public data class ContextParameter(val index: Int) : KdLinkReference()

    @SerialName("typeParameter")
    @Serializable
    public data class TypeParameter(val index: Int) : KdLinkReference()

    @SerialName("package")
    @Serializable
    public data class Package(val packageName: String) : KdLinkReference()

    // save for other things like topic
    @SerialName("module") // at least for java
    @Serializable
    public data class Module(val moduleName: String) : KdLinkReference()

    @SerialName("declaration")
    @Serializable
    public data class Declaration(val declarationId: KdDeclarationId) : KdLinkReference()

}
