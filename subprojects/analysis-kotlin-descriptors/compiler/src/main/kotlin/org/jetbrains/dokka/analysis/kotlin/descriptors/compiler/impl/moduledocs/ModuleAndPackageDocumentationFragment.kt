package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs


internal data class ModuleAndPackageDocumentationFragment(
    val name: String,
    val classifier: ModuleAndPackageDocumentation.Classifier,
    val documentation: String,
    val source: ModuleAndPackageDocumentationSource
)
