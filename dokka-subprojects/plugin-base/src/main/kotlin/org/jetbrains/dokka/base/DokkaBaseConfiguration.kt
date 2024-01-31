/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base

import org.jetbrains.dokka.plugability.ConfigurableBlock
import java.io.File
import java.time.Year

public data class DokkaBaseConfiguration(
    var customStyleSheets: List<File> = defaultCustomStyleSheets,
    var customAssets: List<File> = defaultCustomAssets,
    var separateInheritedMembers: Boolean = separateInheritedMembersDefault,
    var footerMessage: String = defaultFooterMessage,
    var mergeImplicitExpectActualDeclarations: Boolean = mergeImplicitExpectActualDeclarationsDefault,
    var templatesDir: File? = defaultTemplatesDir,
    var homepageLink: String? = null,
) : ConfigurableBlock {
    public companion object {
        public val defaultFooterMessage: String = "© ${Year.now().value} Copyright"
        public val defaultCustomStyleSheets: List<File> = emptyList()
        public val defaultCustomAssets: List<File> = emptyList()
        public const val separateInheritedMembersDefault: Boolean = false
        public const val mergeImplicitExpectActualDeclarationsDefault: Boolean = false
        public val defaultTemplatesDir: File? = null
    }
}
