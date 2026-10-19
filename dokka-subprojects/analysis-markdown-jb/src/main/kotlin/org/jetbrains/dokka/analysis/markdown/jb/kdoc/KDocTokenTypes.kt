/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementType

/** Tags whose first word after the tag name is a subject (parameter name, exception name, ...), not body text. */
internal object KDocTokenTypes {
    val SUBJECT_TAKING_TAGS: Set<String> = setOf("param", "property", "throws", "exception", "see", "sample")

    /**
     * Wraps one `@tag` section - the tag's own line plus everything up to the next top-level `@tag` or EOF - as a
     * single composite AST node, created by [KDocTagSectionMarkerBlock].
     */
    val KDOC_SECTION: IElementType = MarkdownElementType("KDOC_SECTION")

    /**
     * The `@tagName` keyword itself (including the `@`), carved out of a [KDOC_SECTION]'s opening line.
     *
     * `isToken = true` so the block-level tree builder treats it as a plain leaf (its own literal text) instead of
     * a composite - [org.intellij.markdown.parser.TopLevelBuilder] otherwise has no productions of ours spanning
     * this exact range and would fill it with a synthesized [org.intellij.markdown.MarkdownTokenTypes.WHITE_SPACE]
     * child, same as it does for any other "gap" between productions. Unlike [MarkdownElementTypes.PARAGRAPH] (also
     * `isToken = true`), this type isn't in the markdown parser's own inline-expansion allowlist, so it stays a
     * single raw-text leaf rather than being re-parsed for inline markdown syntax - exactly what's wanted for a tag
     * name, which is KDoc syntax, not body content.
     */
    val KDOC_TAG_NAME: IElementType = MarkdownElementType("KDOC_TAG_NAME", isToken = true)

    /**
     * The subject word/`[bracketed]` identifier right after the tag name, for tags in [SUBJECT_TAKING_TAGS] that
     * have one - e.g. `foo` in `@param foo` or `@param[foo]`. Never includes the brackets themselves.
     *
     * `isToken = true` for the same reason as [KDOC_TAG_NAME].
     */
    val KDOC_TAG_SUBJECT: IElementType = MarkdownElementType("KDOC_TAG_SUBJECT", isToken = true)
}
