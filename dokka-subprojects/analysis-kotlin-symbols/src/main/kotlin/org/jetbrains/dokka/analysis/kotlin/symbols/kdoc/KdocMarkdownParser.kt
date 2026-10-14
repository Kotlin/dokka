/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.TokenType
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.checkDecompiledText

/**
 * Splits the raw text of a KDoc comment into tag sections using [MarkdownParser] (backed by
 * [org.jetbrains.dokka.analysis.markdown.jb.kdoc.KDocFlavourDescriptor]) instead of relying on the Kotlin
 * compiler's own [KDocTag]/[KDocSection] structure for that split - the compiler's PSI is only still used
 * here to resolve links (`[Foo]`, and tag subjects like `@param name`) to a [DRI], which genuinely does require
 * real compiler-backed reference resolution and can't be replaced by markdown parsing.
 */
internal fun parseFromKDocTag(
    kDocTag: KDocTag?,
    externalDri: (KDocLink) -> DRI?,
    kdocLocation: String?,
    parseWithChildren: Boolean = true
): DocumentationNode {
    if (kDocTag == null) {
        return DocumentationNode(emptyList())
    }

    // Whether to also pull in sibling tags (e.g. the whole default section's @param/@return/@see/...),
    // or just this one bare tag on its own (used by the Java `{@inheritDoc}` lookup).
    val expandToSiblings = kDocTag.canHaveParent() && parseWithChildren
    val textRoot: PsiElement = if (expandToSiblings) findParent(kDocTag) else kDocTag

    // One shared link map for the whole scope being parsed: covers both inline `[Foo]` references inside tag
    // bodies and tag subjects (`@param name`, `@see Foo`, ...), since a subject's KDocLink is itself a descendant
    // reachable this same way (see KDocTag.getSubjectLink()).
    val links = mutableMapOf<String, KDocLink>()
    textRoot.forEachDescendantOfType<KDocLink> { links[it.getLinkText()] = it }
    val externalDRIProvider = { linkText: String -> links[linkText]?.let(externalDri) }

    val rawText = extractFullKDocText(textRoot)
    val parsed = MarkdownParser(externalDRIProvider, kdocLocation).parse(rawText)

    val tags = if (expandToSiblings) {
        parsed.children
    } else {
        // A single bare tag has no "leading description" of its own; the splitter always synthesizes one
        // (possibly empty) regardless, so drop it to keep the single-tag shape callers expect here.
        parsed.children.filterNot { it is Description }
    }

    return DocumentationNode(resolveSampleTagsSeparately(tags, textRoot))
}

/**
 * All links must be left unresolved in a `@sample` doc section - they're resolved later, from the referenced
 * sample function's own body, by [org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer].
 * Every other tag in this file is resolved through the one shared link map above, so `@sample` tags need their
 * body re-rendered here with a resolver that never resolves anything.
 */
private fun resolveSampleTagsSeparately(tags: List<TagWrapper>, textRoot: PsiElement): List<TagWrapper> {
    if (tags.none { it is Sample }) return tags

    val sampleTagsBySubject = mutableMapOf<String, KDocTag>()
    textRoot.forEachDescendantOfType<KDocTag> { tag ->
        if (tag.knownTag == KDocKnownTag.SAMPLE) {
            tag.getSubjectName()?.let { sampleTagsBySubject[it] = tag }
        }
    }
    if (sampleTagsBySubject.isEmpty()) return tags

    return tags.map { tag ->
        if (tag is Sample) {
            val compilerTag = sampleTagsBySubject[tag.name] ?: return@map tag
            tag.copy(root = MarkdownParser({ null }, null).parseStringToDocNode(compilerTag.getContent()))
        } else tag
    }
}

/**
 * Strips comment decoration (`/**`/`*/`, leading `*` per line) from the whole span covered by [root], producing
 * one flat string with `@tag` markers left in place at the positions the compiler itself recognizes them -
 * ready to be handed to [MarkdownParser]'s own tag-boundary splitter.
 *
 * This walks the real token stream (not a line-based guess), applying the exact same per-token logic as
 * [KDocTag.getContent] - including its code-block/indented-content handling - just without stopping at a
 * [KDocElementTypes.KDOC_TAG] boundary, and flattening through [KDocElementTypes.KDOC_TAG]/[KDocElementTypes
 * .KDOC_SECTION] containers instead. `TAG_NAME` is itself one of [KDocTokens.CONTENT_TOKENS], so tag markers
 * (`@param`, `@return`, ...) come through as ordinary content with no special-casing needed.
 */
private fun extractFullKDocText(root: PsiElement): String {
    val rootNode = root.node ?: return ""

    val builder = StringBuilder()
    val codeBlockBuilder = StringBuilder()
    var targetBuilder = builder

    var contentStarted = false
    var afterAsterisk = false
    var indentedCodeBlock = false

    fun isCodeBlock() = targetBuilder === codeBlockBuilder
    fun startCodeBlock() {
        targetBuilder = codeBlockBuilder
    }

    fun flushCodeBlock() {
        if (isCodeBlock()) {
            builder.append(trimCommonIndent(codeBlockBuilder, indentedCodeBlock))
            codeBlockBuilder.setLength(0)
            targetBuilder = builder
        }
    }

    fun visit(node: ASTNode) {
        val type = node.elementType
        if (type == KDocElementTypes.KDOC_TAG || type == KDocElementTypes.KDOC_SECTION) {
            node.getChildren(null).forEach(::visit)
            return
        }

        val nodeText = node.text
        val isTextIndented = nodeText.isIndentedKDocContent()

        if (type == KDocTokens.CODE_BLOCK_TEXT) {
            indentedCodeBlock = (!isCodeBlock() || indentedCodeBlock) && isTextIndented
            startCodeBlock()
        } else if (KDocTokens.CONTENT_TOKENS.contains(type)) {
            flushCodeBlock()
            indentedCodeBlock = false
        }

        if (KDocTokens.CONTENT_TOKENS.contains(type)) {
            val trimLeadingSpaces = (!contentStarted || afterAsterisk) && !(isCodeBlock() || isTextIndented)
            targetBuilder.append(if (trimLeadingSpaces) nodeText.trimStart() else nodeText)
            contentStarted = true
            afterAsterisk = false
        }
        if (type == KDocTokens.LEADING_ASTERISK) {
            afterAsterisk = true
        }
        if (type == TokenType.WHITE_SPACE) {
            val newlines = StringUtil.countNewLines(nodeText)
            if (newlines > 0) {
                // A line break: collapse to just newlines, dropping any accompanying indentation - the
                // following LEADING_ASTERISK/content token's own trimLeadingSpaces logic handles that.
                if (contentStarted) targetBuilder.append("\n".repeat(newlines))
            } else if (contentStarted) {
                // Pure inline whitespace (e.g. between a tag name and its subject/body) - getContent()
                // never has to handle this because it drops the whitespace right after the tag name
                // before this loop even starts; we don't drop the tag name, so we can't drop this either.
                targetBuilder.append(nodeText)
            }
        }
    }

    rootNode.getChildren(null).forEach(::visit)
    flushCodeBlock()

    // getContent() trims trailing space/tab per tag (it's called once per tag); this builds one string for
    // every tag at once, so the same trim has to be reapplied at each "\n@tag" boundary, not just at the very
    // end - otherwise trailing whitespace on a tag's last line (before the next tag starts) survives. The very
    // end also needs a full trim (not just space/tab): a trailing "\n" there is just the closing "*/" line's
    // own decoration, with no real content after it, same as getContent() never including it to begin with.
    return builder.toString()
        .replace(Regex("[ \t]+(?=\n@)"), "")
        .trimEnd()
}

private val indentationWhiteSpaces = " ".repeat(4)

private fun String.isIndentedKDocContent() = startsWith(indentationWhiteSpaces) || startsWith("\t")

private fun trimCommonIndent(builder: StringBuilder, prepend4WhiteSpaces: Boolean): String {
    val lines = builder.lines()
    val minIndent = lines.filter { it.isNotBlank() }.minOfOrNull { it.indexOfFirst { c -> !c.isWhitespace() } } ?: 0

    return lines.joinToString("\n") { line ->
        if (line.isNotBlank()) {
            line.drop(minIndent).let { if (prepend4WhiteSpaces) indentationWhiteSpaces + it else it }
        } else {
            ""
        }
    }
}

private fun findParent(kDoc: PsiElement): PsiElement =
    if (kDoc.canHaveParent()) findParent(kDoc.parent) else kDoc

private fun PsiElement.canHaveParent(): Boolean = this is KDocSection && knownTag != KDocKnownTag.PROPERTY

// copied-pasted from [org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType]
private inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType({ true }, action)
}

private inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline action: (T) -> Unit
) {
    checkDecompiledText()
    this.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (canGoInside(element)) {
                super.visitElement(element)
            }

            if (element is T) {
                action(element)
            }
        }
    })
}
