package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.RootPageNode

fun interface Renderer {
    fun render(root: RootPageNode)
}