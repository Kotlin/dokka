package org.jetbrains.dokka.base

import org.jetbrains.dokka.plugability.ConfigurableBlock
import java.io.File
import java.time.Year

data class DokkaBaseConfiguration(
    var customStyleSheets: List<File> = defaultCustomStyleSheets,
    var customAssets: List<File> = defaultCustomAssets,
    var separateInheritedMembers: Boolean = separateInheritedMembersDefault,
    var footerMessage: String = defaultFooterMessage
) : ConfigurableBlock {
    companion object {
        val defaultFooterMessage = "© ${Year.now().value} Copyright"
        val defaultCustomStyleSheets: List<File> = emptyList()
        val defaultCustomAssets: List<File> = emptyList()
        const val separateInheritedMembersDefault: Boolean = false
    }
}