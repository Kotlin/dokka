package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.pages.PageNode

interface DocumentationToPageTransformer {
    fun transform (d: DocumentationNode<*>): PageNode
}