package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.pages.PageNode

interface DocumentationToPageTransformer {
    fun transform (modules: Collection<Pair<DokkaConfiguration.PassConfiguration, DocumentationNode<*>>>): PageNode // TODO refactor this
}