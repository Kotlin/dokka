/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.utilities.DokkaLogger

public class FallbackPageMergerStrategy(
    private val logger: DokkaLogger
) : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode> {
        pages.map {
            (it as? ContentPage)
        }
        val renderedPath = path.joinToString(separator = "/")
        if (pages.size != 1) logger.warn("For $renderedPath: expected 1 page, but got ${pages.size}")
        return listOf(pages.first())
    }
}
