package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Model.Module
import org.jetbrains.dokka.pages.PageNode

interface DocumentationToPageTransformer {
    fun transform(passConfiguration: DokkaConfiguration.PassConfiguration, module: Module): PageNode // TODO refactor this... some more?
}