/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.commonmark.CommonMarkMarkerProcessor
import org.intellij.markdown.flavours.gfm.GFMConstraints
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.gfm.table.GitHubTableMarkerProvider
import org.intellij.markdown.parser.LookaheadText
import org.intellij.markdown.parser.MarkerProcessor
import org.intellij.markdown.parser.MarkerProcessorFactory
import org.intellij.markdown.parser.ProductionHolder
import org.intellij.markdown.parser.constraints.MarkdownConstraints
import org.intellij.markdown.parser.constraints.getCharsEaten
import org.intellij.markdown.parser.markerblocks.MarkerBlock
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
import org.intellij.markdown.parser.markerblocks.impl.ParagraphMarkerBlock
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import kotlin.math.min

/**
 * Same provider set [org.intellij.markdown.flavours.gfm.GFMMarkerProcessor] uses (GFM tables, task-list
 * checkboxes), plus [KDocTagMarkerBlockProvider] tried first so that `@tag` lines are recognized before any other
 * provider gets a chance at them.
 *
 * [org.intellij.markdown.flavours.gfm.GFMMarkerProcessor] itself can't be subclassed (it's `final`), so this mirrors
 * it on top of [CommonMarkMarkerProcessor] directly - the same workaround
 * [org.jetbrains.dokka.analysis.java.parsers.doctag.markdown.JavaDocMarkerProcessor] already uses for Javadoc.
 */
internal class KDocMarkerProcessor(productionHolder: ProductionHolder) : CommonMarkMarkerProcessor(
    productionHolder,
    GFMConstraints.BASE
) {
    private val kdocTagMarkerBlockProvider = KDocTagMarkerBlockProvider()

    private val markerBlockProviders = listOf(kdocTagMarkerBlockProvider) +
            super.getMarkerBlockProviders() +
            listOf(GitHubTableMarkerProvider())

    /**
     * The same "does any provider interrupt a paragraph here" predicate [MarkerProcessor] itself builds privately
     * from [getMarkerBlockProviders] - reconstructed here because [createNewMarkerBlocks] needs to hand a
     * [ParagraphMarkerBlock] its own copy, and the base class's version isn't accessible to subclasses.
     */
    private val interruptsParagraph: (LookaheadText.Position, MarkdownConstraints) -> Boolean = { pos, constraints ->
        markerBlockProviders.any { it.interruptsParagraph(pos, constraints) }
    }

    override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> = markerBlockProviders

    /**
     * A `@tag` line needs up to three things created together, atomically, all at this one position:
     *  1. [KDocTokenTypes.KDOC_TAG_NAME] (and [KDocTokenTypes.KDOC_TAG_SUBJECT], if the tag takes one) as their own
     *     leaf nodes, carved directly out of production - *not* left as part of a [MarkdownElementTypes.PARAGRAPH]'s
     *     text the way an earlier version of this override did, since a paragraph's text is handed wholesale to the
     *     inline parser and to [org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser]'s body conversion; the tag
     *     name/subject are KDoc syntax, not body content, and must never show up in either.
     *  2. The [KDocTagSectionMarkerBlock] itself, wrapping both of those and everything that follows up to the next
     *     tag/EOF.
     *  3. If the tag line has trailing content after the name/subject (e.g. "the foo param" in
     *     `@param foo the foo param`), a [ParagraphMarkerBlock] for it, with its start offset forced to begin
     *     *after* that prefix (`productionHolder.updatePosition(contentStart)` before marking it) so the prefix is
     *     excluded from the paragraph's own text/range - while still being the exact same marker instance that then
     *     continues across subsequent lines, preserving normal multi-line paragraph continuation.
     *
     * All of this has to happen in one shot, from here, rather than from [KDocTagMarkerBlockProvider.createMarkerBlocks]:
     * [MarkerProcessor.processPosition] only attempts marker creation once per position, so a lone, zero-width
     * [KDocTagSectionMarkerBlock] pushed by itself would never get a second chance to see the rest of its own
     * opening line - the driver's "jump to the next interesting offset" logic would skip straight over it. And
     * [ParagraphMarkerBlock] needs [interruptsParagraph], which isn't accessible to a plain provider.
     */
    override fun createNewMarkerBlocks(
        pos: LookaheadText.Position,
        productionHolder: ProductionHolder
    ): List<MarkerBlock> {
        val match = kdocTagMarkerBlockProvider.matchTagStart(pos, stateInfo.currentConstraints)
            ?: return super.createNewMarkerBlocks(pos, productionHolder)

        val tagStart = pos.offset
        val sectionMarker = productionHolder.mark()

        val tagNodes = mutableListOf(
            SequentialParser.Node(
                tagStart + match.tagNameRange.first..tagStart + match.tagNameRange.last + 1,
                KDocTokenTypes.KDOC_TAG_NAME
            )
        )
        match.subjectRange?.let { subjectRange ->
            tagNodes += SequentialParser.Node(
                tagStart + subjectRange.first..tagStart + subjectRange.last + 1,
                KDocTokenTypes.KDOC_TAG_SUBJECT
            )
        }
        productionHolder.addProduction(tagNodes)

        val blocks = mutableListOf<MarkerBlock>(
            KDocTagSectionMarkerBlock(stateInfo.currentConstraints, sectionMarker, kdocTagMarkerBlockProvider::interruptsParagraph)
        )

        val contentStart = tagStart + match.contentStart
        val lineEnd = pos.offset + pos.currentLine.length
        if (contentStart < lineEnd) {
            productionHolder.updatePosition(contentStart)
            blocks += ParagraphMarkerBlock(stateInfo.currentConstraints, productionHolder.mark(), interruptsParagraph)
        }

        return blocks
    }

    override fun populateConstraintsTokens(
        pos: LookaheadText.Position,
        constraints: MarkdownConstraints,
        productionHolder: ProductionHolder
    ) {
        if (constraints !is GFMConstraints || !constraints.hasCheckbox()) {
            super.populateConstraintsTokens(pos, constraints, productionHolder)
            return
        }

        val line = pos.currentLine
        var offset = pos.offsetInCurrentLine
        while (offset < line.length && line[offset] != '[') {
            offset++
        }
        if (offset == line.length) {
            super.populateConstraintsTokens(pos, constraints, productionHolder)
            return
        }

        val type = when (constraints.types.lastOrNull()) {
            '>' -> MarkdownTokenTypes.BLOCK_QUOTE
            '.', ')' -> MarkdownTokenTypes.LIST_NUMBER
            else -> MarkdownTokenTypes.LIST_BULLET
        }
        val middleOffset = pos.offset - pos.offsetInCurrentLine + offset
        val endOffset = min(
            pos.offset - pos.offsetInCurrentLine + constraints.getCharsEaten(pos.currentLine),
            pos.nextLineOrEofOffset
        )

        productionHolder.addProduction(
            listOf(
                SequentialParser.Node(pos.offset..middleOffset, type),
                SequentialParser.Node(middleOffset..endOffset, GFMTokenTypes.CHECK_BOX)
            )
        )
    }

    object Factory : MarkerProcessorFactory {
        override fun createMarkerProcessor(productionHolder: ProductionHolder): MarkerProcessor<*> =
            KDocMarkerProcessor(productionHolder)
    }
}
