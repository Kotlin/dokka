package org.jetbrains.dokka.base

import org.jetbrains.dokka.plugability.ConfigurableBlock
import java.io.File

data class DokkaBaseConfiguration(
    var customStyleSheets: List<File> = defaultCustomStyleSheets,
    var customAssets: List<File> = defaultCustomAssets
): ConfigurableBlock {
    companion object {
        val defaultCustomStyleSheets: List<File> = emptyList()
        val defaultCustomAssets: List<File> = emptyList()
    }
}