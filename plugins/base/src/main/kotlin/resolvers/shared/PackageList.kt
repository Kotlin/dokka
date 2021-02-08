package org.jetbrains.dokka.base.resolvers.shared

import org.jetbrains.dokka.base.renderers.PackageListService
import java.net.URL

data class PackageList(
    val linkFormat: RecognizedLinkFormat,
    val packages: Set<String>,
    val locations: Map<String, String>,
    val url: URL
) {
    companion object {
        fun load(url: URL, jdkVersion: Int, offlineMode: Boolean = false): PackageList? {
            if (offlineMode && url.protocol.toLowerCase() != "file")
                return null

            val packageListStream = kotlin.runCatching { url.readContent() }.onFailure {
                println("Failed to download package-list from $url, this might suggest that remote resource is not available," +
                        " module is empty or dokka output got corrupted")
                return null
            }.getOrThrow()

            val (params, packages) = packageListStream
                .bufferedReader()
                .useLines { lines -> lines.partition { it.startsWith(PackageListService.DOKKA_PARAM_PREFIX) } }

            val paramsMap = splitParams(params)
            val format = linkFormat(paramsMap["format"]?.singleOrNull(), jdkVersion)
            val locations = splitLocations(paramsMap["location"].orEmpty()).filterKeys(String::isNotEmpty)

            return PackageList(format, packages.filter(String::isNotBlank).toSet(), locations, url)
        }

        private fun splitParams(params: List<String>) = params.asSequence()
            .map { it.removePrefix("${PackageListService.DOKKA_PARAM_PREFIX}.").split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        private fun splitLocations(locations: List<String>) = locations.map { it.split("\u001f", limit = 2) }
            .map { (key, value) -> key to value }
            .toMap()

        private fun linkFormat(formatName: String?, jdkVersion: Int) =
            formatName?.let { RecognizedLinkFormat.fromString(it) }
                ?: when {
                    jdkVersion < 8 -> RecognizedLinkFormat.Javadoc1 // Covers JDK 1 - 7
                    jdkVersion < 10 -> RecognizedLinkFormat.Javadoc8 // Covers JDK 8 - 9
                    else -> RecognizedLinkFormat.Javadoc10 // Covers JDK 10+
                }
    }
}
