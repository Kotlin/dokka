package org.jetbrains.dokka.analysis

import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies

/** TODO: replace by [NativeKlibLibraryInfo] after fix of KT-40734 */
internal class DokkaNativeKlibLibraryDependencyResolver {
    private val cachedDependencies = mutableMapOf</* libraryName */String, DokkaNativeKlibLibraryInfo>()

    fun registerLibrary(libraryInfo: DokkaNativeKlibLibraryInfo) {
        cachedDependencies[libraryInfo.kotlinLibrary.uniqueName] = libraryInfo
    }

    fun resolveDependencies(libraryInfo: DokkaNativeKlibLibraryInfo): List<DokkaNativeKlibLibraryInfo> {
        return libraryInfo.kotlinLibrary.unresolvedDependencies.mapNotNull { cachedDependencies[it.path] }
    }
}
