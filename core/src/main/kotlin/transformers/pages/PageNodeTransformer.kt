package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

abstract class PageNodeTransformer(context: DokkaContext? = null) {
    abstract operator fun invoke(input: RootPageNode): RootPageNode
}