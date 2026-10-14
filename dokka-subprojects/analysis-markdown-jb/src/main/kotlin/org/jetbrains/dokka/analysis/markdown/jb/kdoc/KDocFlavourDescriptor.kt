/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkerProcessorFactory

/**
 * [GFMFlavourDescriptor] extended with recognition of KDoc `@tag` sections as block-level markdown constructs
 * (see [KDocTagMarkerBlockProvider]), so that tag-section splitting is done by the markdown parser itself instead
 * of a separate, bespoke text scanner.
 */
internal class KDocFlavourDescriptor : GFMFlavourDescriptor() {
    override val markerProcessorFactory: MarkerProcessorFactory
        get() = KDocMarkerProcessor.Factory
}
