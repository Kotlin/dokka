package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaModuleContext

interface DocumentableMerger {
    operator fun invoke(modules: Collection<DModule>, context: DokkaModuleContext): DModule
}
