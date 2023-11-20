/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.shortName
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import java.io.IOException

/** TODO: replace by [NativeKlibLibraryInfo] after fix of KT-40734 */
internal class DokkaNativeKlibLibraryInfo(
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

    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform
    override fun dependencies(): List<ModuleInfo> = listOf(this) + dependencyResolver.resolveDependencies(this)
    override fun getLibraryRoots(): Collection<String> = listOf(libraryRoot)

    override val capabilities: Map<ModuleCapability<*>, Any?>
        get() {
            val capabilities = super.capabilities.toMutableMap()
            capabilities[KlibModuleOrigin.CAPABILITY] = DeserializedKlibModuleOrigin(kotlinLibrary)
            capabilities[ImplicitIntegerCoercion.MODULE_CAPABILITY] = kotlinLibrary.safeRead(false) { isInterop }
            return capabilities
        }

    private fun <T> KotlinLibrary.safeRead(defaultValue: T, action: KotlinLibrary.() -> T) = try {
        action()
    } catch (_: IOException) {
        defaultValue
    }
}
