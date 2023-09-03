/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.shared

import java.net.URL

public typealias Module = String

public data class PackageList(
    val linkFormat: RecognizedLinkFormat,
    val modules: Map<Module, Set<String>>,
    val locations: Map<String, String>,
    val url: URL
) {
    val packages: Set<String>
        get() = modules.values.flatten().toSet()

    public fun moduleFor(packageName: String): Module? {
        return modules.asSequence()
            .filter { it.value.contains(packageName) }
            .firstOrNull()?.key
    }

    public companion object {
        public const val PACKAGE_LIST_NAME: String = "package-list"
        public const val MODULE_DELIMITER: String = "module:"
        public const val DOKKA_PARAM_PREFIX: String = "\$dokka"
        public const val SINGLE_MODULE_NAME: String = ""

        public fun load(url: URL, jdkVersion: Int, offlineMode: Boolean = false): PackageList? {
            if (offlineMode && url.protocol.toLowerCase() != "file")
                return null

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
