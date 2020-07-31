package org.jetbrains.dokka.analysis

import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.idea.klib.safeRead
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.shortName
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

/** TODO: replace by [NativeKlibLibraryInfo] after fix of KT-40734 */
internal class DokkaNativeKlibLibraryInfo(
    val kotlinLibrary: KotlinLibrary,
    override val analyzerServices: PlatformDependentAnalyzerServices,
    private val dependencyResolver: DokkaNativeKlibLibraryDependencyResolver
) : LibraryModuleInfo {
    init {
        dependencyResolver.registerLibrary(this)
    }

    internal val libraryRoot: String
        get() = kotlinLibrary.libraryFile.path

    override val name: Name by lazy {
        val libraryName = kotlinLibrary.shortName ?: kotlinLibrary.uniqueName
        Name.special("<$libraryName>")
    }

    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform
    override fun dependencies(): List<ModuleInfo> = listOf(this) + dependencyResolver.resolveDependencies(this)
    override fun getLibraryRoots(): Collection<String> = listOf(libraryRoot)

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() {
            val capabilities = super.capabilities.toMutableMap()
            capabilities += KlibModuleOrigin.CAPABILITY to DeserializedKlibModuleOrigin(kotlinLibrary)
            capabilities += ImplicitIntegerCoercion.MODULE_CAPABILITY to kotlinLibrary.safeRead(false) { isInterop }
            return capabilities
        }
}
