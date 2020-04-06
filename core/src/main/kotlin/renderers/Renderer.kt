package org.jetbrains.dokka.renderers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.jetbrains.dokka.pages.RootPageNode

interface Renderer {
    fun CoroutineScope.render(root: RootPageNode): Job
}