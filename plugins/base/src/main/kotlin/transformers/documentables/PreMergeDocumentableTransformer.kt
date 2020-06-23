package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule

interface PreMergeDocumentableTransformer {
    operator fun invoke(modules: List<DModule>): List<DModule>
}