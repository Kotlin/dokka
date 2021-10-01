package org.jetbrains.dokka.analysis.resolve

import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies

/** TODO: replace by [NativeKlibLibraryInfo] after fix of KT-40734 */
internal class DokkaKlibLibraryDependencyResolver {
    private val cachedDependencies = mutableMapOf</* libraryName */String, DokkaKlibLibraryInfo>()

    fun registerLibrary(libraryInfo: DokkaKlibLibraryInfo) {
        cachedDependencies[libraryInfo.kotlinLibrary.uniqueName] = libraryInfo
    }

    fun resolveDependencies(libraryInfo: DokkaKlibLibraryInfo): List<DokkaKlibLibraryInfo> {
        return libraryInfo.kotlinLibrary.unresolvedDependencies.mapNotNull { cachedDependencies[it.path] }
    }
}
