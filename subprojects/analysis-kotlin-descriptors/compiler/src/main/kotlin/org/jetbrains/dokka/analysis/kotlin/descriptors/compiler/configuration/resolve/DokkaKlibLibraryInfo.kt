package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.library.KotlinLibrary

internal abstract class DokkaKlibLibraryInfo : LibraryModuleInfo {
    abstract val kotlinLibrary: KotlinLibrary
    internal val libraryRoot: String
        get() = kotlinLibrary.libraryFile.path
}
