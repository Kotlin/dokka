package org.jetbrains.dokka.base.resolvers.shared

import org.jetbrains.dokka.base.renderers.PackageListService
import org.jetbrains.dokka.plugability.DokkaContext
import java.net.URL

data class PackageList(
    val linkFormat: RecognizedLinkFormat,
    val packages: Set<String>,
    val locations: Map<String, String>,
    val url: URL
) {
    companion object {
        fun load(url: URL, jdkVersion: Int, dokkaContext: DokkaContext): PackageList? {
            if (dokkaContext.configuration.offlineMode && url.protocol.toLowerCase() != "file")
                return null

            val packageListStream = url.doOpenConnectionToReadContent().getInputStream()
            val (params, packages) =
                packageListStream
                    .bufferedReader()
                    .useLines { lines -> lines.partition { it.startsWith(PackageListService.DOKKA_PARAM_PREFIX) } }

            val paramsMap = params.asSequence()
                .map { it.removePrefix("${PackageListService.DOKKA_PARAM_PREFIX}.").split(":", limit = 2) }
                .groupBy({ (key, _) -> key }, { (_, value) -> value })

            val format = paramsMap["format"]?.singleOrNull()?.let { RecognizedLinkFormat.fromString(it) } ?: when {
                jdkVersion < 8 -> RecognizedLinkFormat.Javadoc1 // Covers JDK 1 - 7
                jdkVersion < 10 -> RecognizedLinkFormat.Javadoc8 // Covers JDK 8 - 9
                else -> RecognizedLinkFormat.Javadoc10 // Covers JDK 10+
            }

            val locations = paramsMap["location"].orEmpty()
                .map { it.split("\u001f", limit = 2) }
                .map { (key, value) -> key to value }
                .toMap()

            return PackageList(format, packages.toSet(), locations, url)
        }
    }
}