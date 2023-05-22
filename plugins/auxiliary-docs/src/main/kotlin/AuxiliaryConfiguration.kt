package org.jetbrains.dokka.auxiliaryDocs

import org.jetbrains.dokka.plugability.ConfigurableBlock
import java.io.File

data class AuxiliaryConfiguration(
    var entryPointNode: File? = defaultEntryPointNode,
    var nodesDir: File? = defaultNodes,
    var apiReferenceNodeName: String? = defaultApiReferenceNodeName,
) : ConfigurableBlock {
    companion object {
        val defaultEntryPointNode: File? = null
        val defaultNodes: File? = null
        val defaultApiReferenceNodeName: String? = null
    }
}