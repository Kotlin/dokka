package org.jetbrains.dokka.base.parsers.moduleAndPackage

import org.jetbrains.dokka.DokkaException

internal class IllegalModuleAndPackageDocumentation(
    source: ModuleAndPackageDocumentationSource, message: String
) : DokkaException("[$source] $message")
