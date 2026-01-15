/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag.markdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkerProcessorFactory

internal class JavaDocMarkdownFlavourDescriptor : GFMFlavourDescriptor(
    useSafeLinks = true, absolutizeAnchorLinks = false, makeHttpsAutoLinks = true
) {
    override fun createHtmlGeneratingProviders(
        linkMap: LinkMap,
        baseURI: URI?
    ): MutableMap<IElementType, GeneratingProvider> =
        super.createHtmlGeneratingProviders(linkMap, baseURI).toMutableMap().apply {
            // Don't generate a body
            remove(MarkdownElementTypes.MARKDOWN_FILE)
        }

    override val markerProcessorFactory: MarkerProcessorFactory
        get() = JavaDocMarkerProcessorFactory()
}
