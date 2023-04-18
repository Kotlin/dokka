package org.jetbrains.dokka.auxiliaryDocs

import org.jetbrains.dokka.plugability.ConfigurableBlock
import java.io.File

data class AuxiliaryConfiguration(
    var docs: Set<File> = defaultDocs,
) : ConfigurableBlock {
    companion object {
        val defaultDocs: Set<File> = emptySet()
    }
}