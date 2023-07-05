package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs

import org.jetbrains.dokka.DokkaException

internal class IllegalModuleAndPackageDocumentation(
    source: ModuleAndPackageDocumentationSource, message: String
) : DokkaException("[$source] $message")
