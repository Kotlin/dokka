package org.jetbrains.dokka.base.parsers.moduleAndPackage

import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.*

data class ModuleAndPackageDocumentationFragment(
    val name: String,
    val classifier: Classifier,
    val documentation: String,
    val source: ModuleAndPackageDocumentationSource
)
