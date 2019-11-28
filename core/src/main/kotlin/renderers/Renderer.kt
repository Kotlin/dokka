package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.PageNode

interface Renderer {
    fun render(root: PageNode)
}