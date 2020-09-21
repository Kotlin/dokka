package org.jetbrains.dokka.base.parsers.moduleAndPackage

import java.io.File

abstract class ModuleAndPackageDocumentationSource {
    abstract val sourceDescription: String
    abstract val documentation: String
    override fun toString(): String = sourceDescription
}

internal data class ModuleAndPackageDocumentationFile(private val file: File) : ModuleAndPackageDocumentationSource() {
    override val sourceDescription: String = file.path
    override val documentation: String by lazy(LazyThreadSafetyMode.PUBLICATION) { file.readText() }
}
