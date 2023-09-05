/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.RootPageNode

internal fun shouldRenderSourceSetTabs(page: RootPageNode): Boolean {
    return page.withDescendants()
        .flatMap { pageNode ->
            if (pageNode is ContentPage) pageNode.content.withDescendants()
            else emptySequence()
        }
        .flatMap { contentNode -> contentNode.sourceSets }
        .distinct()
        .count() > 1
}
