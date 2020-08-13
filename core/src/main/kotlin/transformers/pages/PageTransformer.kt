package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode

interface PageTransformer {
    operator fun invoke(input: RootPageNode): RootPageNode
}
