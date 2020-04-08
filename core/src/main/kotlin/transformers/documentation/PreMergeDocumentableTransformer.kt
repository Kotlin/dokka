package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModuleView
import org.jetbrains.dokka.model.DPass
import org.jetbrains.dokka.plugability.DokkaContext

interface PreMergeDocumentableTransformer {
    operator fun invoke(moduleViews: List<DPass>, context: DokkaContext): List<DPass>
}