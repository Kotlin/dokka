package org.jetbrains.dokka.analysis.resolve

import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.library.KotlinLibrary

abstract class DokkaKlibLibraryInfo : LibraryModuleInfo {
    abstract val kotlinLibrary: KotlinLibrary
    internal val libraryRoot: String
        get() = kotlinLibrary.libraryFile.path
}