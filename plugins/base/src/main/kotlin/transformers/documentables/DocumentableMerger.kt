package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.base.plugability.DokkaContext
import org.jetbrains.dokka.model.DModule

interface DocumentableMerger {
    operator fun invoke(modules: Collection<DModule>, context: DokkaContext): DModule
}