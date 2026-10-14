/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

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
import org.intellij.markdown.parser.markerblocks.MarkerBlockProvider
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
    private val markerBlockProviders = listOf(KDocTagMarkerBlockProvider()) +
            super.getMarkerBlockProviders() +
            listOf(GitHubTableMarkerProvider())

    override fun getMarkerBlockProviders(): List<MarkerBlockProvider<StateInfo>> = markerBlockProviders

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
