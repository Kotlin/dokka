package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

interface PageCreator {
    operator fun invoke(): RootPageNode
}