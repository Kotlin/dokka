/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.shared

import java.net.URL
import java.util.concurrent.ConcurrentHashMap

public typealias Module = String

public data class PackageList(
    val linkFormat: RecognizedLinkFormat,
    val modules: Map<Module, Set<String>>,
    val locations: Map<String, String>,
    val url: URL
) {
    private val moduleByPackage: Map<String, Module> by lazy {
        mutableMapOf<String, Module>().apply {
            modules.forEach { (module, packages) ->
                packages.forEach { pkg ->
                    // if multiple modules have the same package (split-package), store the first module.
                    if (!containsKey(pkg)) put(pkg, module)
                }
            }
        }
    }

    val packages: Set<String> get() = moduleByPackage.keys

    public fun moduleFor(packageName: String): Module? = moduleByPackage[packageName]

    public companion object {
        public const val PACKAGE_LIST_NAME: String = "package-list"
        public const val MODULE_DELIMITER: String = "module:"
        public const val DOKKA_PARAM_PREFIX: String = "\$dokka"
        public const val SINGLE_MODULE_NAME: String = ""

        private val cache: ConcurrentHashMap<Pair<URL, Int>, PackageList> = ConcurrentHashMap()

        public fun clearCache() {
            cache.clear()
        }

        /**
         * During the Dokka execution pipeline, usually, there exists only several instances of `PackageList`.
         * Those are created to resolve links to external documentation, or in the case of multi-module documentation
         * to resolve links to other modules.
         * But because of the way Dokka works, it calls `PackageList.load` multiple times with the same URL.
         * This causes downloading the same file again and again.
         *
         * So to avoid unnecessary computations and save a lot of CPU resources, we cache it.
         *
         * [clearCache] is used to clear the cache.
         * It is called via [org.jetbrains.dokka.renderers.PostAction] in both single and multi-module [org.jetbrains.dokka.generation.Generation] pipelines
         */
        public fun load(url: URL, jdkVersion: Int, offlineMode: Boolean = false): PackageList? {
            if (offlineMode && url.protocol.toLowerCase() != "file")
                return null

            return cache.getOrPut(url to jdkVersion) {
                // we cache nothing on failure (when `download` returns null)
                download(url, jdkVersion) ?: return null
            }
        }

        internal fun loadWithoutCache(url: URL, jdkVersion: Int, offlineMode: Boolean = false): PackageList? {
            if (offlineMode && url.protocol.toLowerCase() != "file")
                return null

            return download(url, jdkVersion)
        }

        private fun download(url: URL, jdkVersion: Int): PackageList? {
            val packageListStream = runCatching { url.readContent() }.onFailure {
                println("Failed to download package-list from $url, this might suggest that remote resource is not available," +
                        " module is empty or dokka output got corrupted")
                return null
            }.getOrThrow()

            val (params, packages) = packageListStream
                .bufferedReader()
                .useLines { lines -> lines.partition { it.startsWith(DOKKA_PARAM_PREFIX) } }

            val paramsMap = splitParams(params)
            val format = linkFormat(paramsMap["format"]?.singleOrNull(), jdkVersion)
            val locations = splitLocations(paramsMap["location"].orEmpty()).filterKeys(String::isNotEmpty)

            val modulesMap = splitPackages(packages)
            return PackageList(format, modulesMap, locations, url)
        }

        private fun splitParams(params: List<String>) = params.asSequence()
            .map { it.removePrefix("$DOKKA_PARAM_PREFIX.").split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        private fun splitLocations(locations: List<String>) = locations.map { it.split("\u001f", limit = 2) }
                .associate { (key, value) -> key to value }

        private fun splitPackages(packages: List<String>): Map<Module, Set<String>> =
                packages.fold(("" to mutableMapOf<Module, Set<String>>())) { (lastModule, acc), el ->
                    val currentModule : String
                    when {
                        el.startsWith(MODULE_DELIMITER) -> currentModule = el.substringAfter(MODULE_DELIMITER)
                        el.isNotBlank() -> {
                            currentModule = lastModule
                            acc[currentModule] = acc.getOrDefault(lastModule, emptySet()) + el
                        }
                        else -> currentModule = lastModule
                    }
                    currentModule to acc
                }.second

        private fun linkFormat(formatName: String?, jdkVersion: Int) =
            formatName?.let { RecognizedLinkFormat.fromString(it) }
                ?: when {
                    jdkVersion < 8 -> RecognizedLinkFormat.Javadoc1 // Covers JDK 1 - 7
                    jdkVersion < 10 -> RecognizedLinkFormat.Javadoc8 // Covers JDK 8 - 9
                    else -> RecognizedLinkFormat.Javadoc10 // Covers JDK 10+
                }
    }
}
