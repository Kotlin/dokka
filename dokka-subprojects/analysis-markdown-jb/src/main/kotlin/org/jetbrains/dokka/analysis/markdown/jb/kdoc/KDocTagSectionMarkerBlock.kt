/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.intellij.markdown.IElementType
import org.intellij.markdown.parser.LookaheadText
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.constraints.applyToNextLineAndAddModifiers
import org.intellij.markdown.parser.constraints.getCharsEaten
import org.intellij.markdown.parser.markerblocks.MarkerBlock
import org.intellij.markdown.parser.markerblocks.MarkerBlockImpl

/**
 * Owns everything from one `@tag` line up to (but not including) the next top-level `@tag` line or EOF, modeled
 * directly on [org.intellij.markdown.parser.markerblocks.impl.BlockQuoteMarkerBlock]: a composite block
 * (`allowsSubBlocks() == true`) that lets the ordinary block providers - paragraphs, lists, code fences, tables -
 * parse its content as real nested children, and decides whether it's done at each end-of-line exactly the way
 * [org.intellij.markdown.parser.markerblocks.impl.ParagraphMarkerBlock] decides whether it's done: by asking
 * [interruptsParagraph] about the upcoming line.
 *
 * [interruptsParagraph] is deliberately narrower here than the predicate the body's own paragraphs are given (see
 * [org.jetbrains.dokka.analysis.markdown.jb.kdoc.KDocMarkerProcessor]) - it only fires for another top-level
 * `@tag`, not for "any provider interrupts a paragraph" - so a table, list, heading, etc. appearing inside a tag's
 * body ends that paragraph (as normal) without ending the *section*.
 *
 * This block never produces any children of its own in [org.intellij.markdown.parser.markerblocks.MarkerBlockImpl]
 * - unlike the single-line `KDocTagMarkerBlock` this replaces, it doesn't try to carve the `@tagName subject`
 * prefix out of the AST itself. That extraction still happens afterward, in [org.jetbrains.dokka.analysis.markdown
 * .jb.MarkdownParser], by re-applying [KDocTagMarkerBlockProvider.parseTagStart] to this section's own start
 * offset and trimming the matched prefix off its first child - the same text-offset-based approach already used
 * before this block existed, just applied per-section instead of across the whole flat top-level child list.
 */
internal class KDocTagSectionMarkerBlock(
    constraints: MarkdownConstraints,
    marker: ProductionHolder.Marker,
    private val interruptsParagraph: (LookaheadText.Position, MarkdownConstraints) -> Boolean
) : MarkerBlockImpl(constraints, marker) {

    override fun allowsSubBlocks(): Boolean = true

    override fun isInterestingOffset(pos: LookaheadText.Position): Boolean = pos.offsetInCurrentLine == -1

    override fun calcNextInterestingOffset(pos: LookaheadText.Position): Int = pos.nextLineOffset ?: -1

    override fun getDefaultAction(): MarkerBlock.ClosingAction = MarkerBlock.ClosingAction.DONE

    override fun getDefaultNodeType(): IElementType = KDocTokenTypes.KDOC_SECTION

    override fun doProcessToken(pos: LookaheadText.Position, currentConstraints: MarkdownConstraints): MarkerBlock.ProcessingResult {
        val nextLineConstraints = constraints.applyToNextLineAndAddModifiers(pos)
        val posToCheck = pos.nextPosition(1 + nextLineConstraints.getCharsEaten(pos.currentLine))
        if (posToCheck == null || interruptsParagraph(posToCheck, nextLineConstraints)) {
            return MarkerBlock.ProcessingResult.DEFAULT
        }
        return MarkerBlock.ProcessingResult.PASS
    }
}
