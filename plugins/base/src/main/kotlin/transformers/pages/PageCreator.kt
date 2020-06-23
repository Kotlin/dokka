package org.jetbrains.dokka.base.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode

interface PageCreator {
    operator fun invoke(): RootPageNode
}