/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.shortName
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

/** TODO: replace by [org.jetbrains.kotlin.caches.resolve.JsKlibLibraryInfo] after fix of KT-40734 */
internal class DokkaJsKlibLibraryInfo(
    override val kotlinLibrary: KotlinLibrary,
    override val analyzerServices: PlatformDependentAnalyzerServices,
    private val dependencyResolver: DokkaKlibLibraryDependencyResolver
) : DokkaKlibLibraryInfo() {
    init {
        dependencyResolver.registerLibrary(this)
    }

    override val name: Name by lazy {
        val libraryName = kotlinLibrary.shortName ?: kotlinLibrary.uniqueName
        Name.special("<$libraryName>")
    }

    override val platform: TargetPlatform = JsPlatforms.defaultJsPlatform
    override fun dependencies(): List<ModuleInfo> = listOf(this) + dependencyResolver.resolveDependencies(this)
    override fun getLibraryRoots(): Collection<String> = listOf(libraryRoot)
}
