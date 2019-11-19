package org.jetbrains.dokka.pages.transformers

import org.jetbrains.dokka.pages.ModulePageNode

interface PageNodeTransformer {
    operator fun invoke(original: ModulePageNode): ModulePageNode
}