package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.Model.Module
import org.jetbrains.dokka.plugability.DokkaContext

interface DocumentationNodeMerger {
    operator fun invoke(modules: Collection<Module>, context: DokkaContext): Module
}