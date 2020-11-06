package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentPage

interface CustomContentNodeRenderer {

    /**
     * @return true if this renderer can render the [node], false otherwise
     */
    fun <T: ContentNode> canRender(node: T) : Boolean

    fun <T, R: ContentNode> render(builder: T, node: R, pageContext: ContentPage, sourceSetRestriction: Set<DisplaySourceSet>?)
}
