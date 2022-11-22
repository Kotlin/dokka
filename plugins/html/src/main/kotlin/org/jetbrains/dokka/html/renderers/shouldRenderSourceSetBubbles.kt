package org.jetbrains.dokka.html.renderers

import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.RootPageNode

internal fun shouldRenderSourceSetBubbles(page: RootPageNode): Boolean {
    return page.withDescendants()
        .flatMap { pageNode ->
            if (pageNode is ContentPage) pageNode.content.withDescendants()
            else emptySequence()
        }
        .flatMap { contentNode -> contentNode.sourceSets }
        .distinct()
        .count() > 1
}
