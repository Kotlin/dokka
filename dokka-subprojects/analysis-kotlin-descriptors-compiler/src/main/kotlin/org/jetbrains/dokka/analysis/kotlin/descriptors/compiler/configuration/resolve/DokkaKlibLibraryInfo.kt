/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.library.KotlinLibrary

internal abstract class DokkaKlibLibraryInfo : LibraryModuleInfo {
    abstract val kotlinLibrary: KotlinLibrary
    internal val libraryRoot: String
        get() = kotlinLibrary.libraryFile.path
}
