package org.jetbrains.dokka.Model.transformers

import org.jetbrains.dokka.Model.Module

interface DocumentationNodeTransformer {
    operator fun invoke(original: Module): Module
}