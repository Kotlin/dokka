package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaBaseContext
import org.jetbrains.dokka.plugability.DokkaModuleContext

interface PageTransformer {
    operator fun invoke(input: RootPageNode, context: DokkaBaseContext): RootPageNode
}
