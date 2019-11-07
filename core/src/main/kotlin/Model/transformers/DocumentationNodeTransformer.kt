package org.jetbrains.dokka.Model.transformers

import org.jetbrains.dokka.Model.Module

interface DocumentationNodeTransformer {
    operator fun invoke(original: Module): Module
    operator fun invoke(modules: Collection<Module>): Module
}