package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.RootPageNode

interface CreationContext

object NoCreationContext : CreationContext

interface PageCreator<T: CreationContext> {
    operator fun invoke(creationContext: T): RootPageNode
}