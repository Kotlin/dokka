/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.intellij.markdown.parser.LookaheadText
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.markerblocks.MarkerBlock
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider

/**
 * Recognizes a line starting with `@tagName` as the beginning of a new KDoc tag section. Block/AST-node creation
 * for it (a [KDocTagSectionMarkerBlock] plus the [KDOC_TAG_NAME][KDocTokenTypes.KDOC_TAG_NAME]/
 * [KDOC_TAG_SUBJECT][KDocTokenTypes.KDOC_TAG_SUBJECT] leaves carved out of its own opening line) happens in
 * [KDocMarkerProcessor.createNewMarkerBlocks] instead of here - see that function's own doc comment for why. This
 * provider only owns the tag-matching regex itself ([matchTagStart]/[parseTagStart]), shared between
 * [interruptsParagraph] (used to end the *preceding* paragraph and, with a narrower composed predicate, the
 * preceding *section*) and [KDocMarkerProcessor] (used to recognize where a *new* section begins).
 *
 * Only triggers at the comment's top level (`constraints.indent == 0`, i.e. not inside a list or blockquote) and
 * only when `@` is the very first character of the line (no leading indentation at all), matching the syntax real
 * KDoc comments use. Because this is an ordinary [MarkerBlockProvider], a `@` occurring inside an open fenced code
 * block is never offered to it by the surrounding parser - fences (and lists, and blockquotes) are already handled
 * correctly by the library itself, with no bespoke tracking needed here.
 */
internal class KDocTagMarkerBlockProvider : MarkerBlockProvider<MarkerProcessor.StateInfo> {

    override fun createMarkerBlocks(
        pos: LookaheadText.Position,
        productionHolder: ProductionHolder,
        stateInfo: MarkerProcessor.StateInfo
    ): List<MarkerBlock> = emptyList()

    override fun interruptsParagraph(pos: LookaheadText.Position, constraints: MarkdownConstraints): Boolean =
        matchTagStart(pos, constraints) != null

    internal fun matchTagStart(pos: LookaheadText.Position, constraints: MarkdownConstraints): TagStartMatch? {
        if (constraints.indent != 0) return null
        if (pos.offsetInCurrentLine != 0) return null
        return parseTagStart(pos.currentLineFromPosition)
    }

    /**
     * [subjectRange] never includes enclosing `[`/`]` when the subject was written as `@tag[subject]`
     * (brackets directly attached to the tag name, or separated by whitespace) rather than a bare word.
     */
    internal class TagStartMatch(val tagNameRange: IntRange, val subjectRange: IntRange?, val contentStart: Int)

    internal companion object {
        // Tag names are identifiers, same as KDocLexer's TAG_NAME token - this must NOT be `\S+`, since a
        // bracketed subject can immediately follow with no space (`@param[name]`).
        private val TAG_NAME_REGEX = Regex("^@([a-zA-Z][a-zA-Z0-9_]*)")

        internal fun parseTagStart(line: CharSequence): TagStartMatch? {
            val nameMatch = TAG_NAME_REGEX.find(line) ?: return null
            val nameGroup = nameMatch.groups[1] ?: return null
            val tagName = nameGroup.value
            val afterName = nameGroup.range.last + 1

            if (tagName !in KDocTokenTypes.SUBJECT_TAKING_TAGS) {
                return TagStartMatch(0 until afterName, null, skipHorizontalWhitespace(line, afterName))
            }

            var subjectStart = afterName
            while (subjectStart < line.length && line[subjectStart].isHorizontalWhitespace()) subjectStart++

            // `@tag[subject]` - brackets either directly attached to the tag name or whitespace-separated.
            if (subjectStart < line.length && line[subjectStart] == '[') {
                val closing = line.indexOf(']', subjectStart + 1)
                if (closing != -1) {
                    return TagStartMatch(
                        0 until afterName,
                        (subjectStart + 1) until closing,
                        skipHorizontalWhitespace(line, closing + 1)
                    )
                }
            }

            if (subjectStart >= line.length) {
                return TagStartMatch(0 until afterName, null, skipHorizontalWhitespace(line, afterName))
            }

            var subjectEnd = subjectStart
            while (subjectEnd < line.length && !line[subjectEnd].isWhitespace()) subjectEnd++

            return TagStartMatch(
                0 until afterName,
                subjectStart until subjectEnd,
                skipHorizontalWhitespace(line, subjectEnd)
            )
        }

        private fun skipHorizontalWhitespace(line: CharSequence, from: Int): Int {
            var offset = from
            while (offset < line.length && line[offset].isHorizontalWhitespace()) offset++
            return offset
        }

        private fun Char.isHorizontalWhitespace(): Boolean = this == ' ' || this == '\t'
    }
}
