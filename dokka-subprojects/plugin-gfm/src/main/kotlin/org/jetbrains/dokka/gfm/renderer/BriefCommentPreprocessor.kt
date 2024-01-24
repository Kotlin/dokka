/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gfm.renderer

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class BriefCommentPreprocessor : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        return input.transformContentPagesTree { contentPage ->
            contentPage.modified(content = contentPage.content.recursiveMapTransform<ContentGroup, ContentNode> {
                if (it.dci.kind == ContentKind.BriefComment) {
                    it.copy(style = it.style + setOf(TextStyle.Block))
                } else {
                    it
                }
            })
        }
    }
}
