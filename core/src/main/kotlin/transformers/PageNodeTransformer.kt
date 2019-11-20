package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext

interface PageNodeTransformer {
    fun invoke(input: ModulePageNode, dokkaContext: DokkaContext): ModulePageNode
}