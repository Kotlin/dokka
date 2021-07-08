package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.RootPageNode

interface Renderer {
    /**
     * Extension of the output format without dot at the beginning
     */
    val outputExtension: String
    fun render(root: RootPageNode)
}