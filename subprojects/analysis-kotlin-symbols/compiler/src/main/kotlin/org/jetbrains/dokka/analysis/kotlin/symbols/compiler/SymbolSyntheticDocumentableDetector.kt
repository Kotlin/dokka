package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.kotlin.analysis.kotlin.internal.SyntheticDocumentableDetector

class SymbolSyntheticDocumentableDetector : SyntheticDocumentableDetector {
    override fun isSynthetic(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        TODO("Not yet implemented")
    }

}
