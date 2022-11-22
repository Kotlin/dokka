package org.jetbrains.dokka.html

import org.jetbrains.dokka.plugability.ConfigurableBlock
import java.io.File

data class DokkaHtmlConfiguration(
    var customStyleSheets: List<File> = defaultCustomStyleSheets,
    var customAssets: List<File> = defaultCustomAssets,
    var mergeImplicitExpectActualDeclarations: Boolean = mergeImplicitExpectActualDeclarationsDefault,
    var templatesDir: File? = defaultTemplatesDir
) : ConfigurableBlock {
    companion object {
        val defaultCustomStyleSheets: List<File> = emptyList()
        val defaultCustomAssets: List<File> = emptyList()
        const val mergeImplicitExpectActualDeclarationsDefault: Boolean = false
        val defaultTemplatesDir: File? = null
    }
}