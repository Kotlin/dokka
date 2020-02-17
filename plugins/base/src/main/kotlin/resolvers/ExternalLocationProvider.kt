package org.jetbrains.dokka.base.resolvers

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import org.jetbrains.dokka.links.DRI
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

object ExternalLocationProvider { // TODO: Refactor this!!!
    private const val DOKKA_PARAM_PREFIX = "\$dokka."

    private val cache: MutableMap<URL, LocationInfo> = mutableMapOf()

    fun getLocation(dri: DRI, externalDocumentationLinks: List<ExternalDocumentationLink>): String {
        val toResolve: MutableList<ExternalDocumentationLink> = mutableListOf()
        for(link in externalDocumentationLinks){
            val info = cache[link.packageListUrl]
            if(info == null) {
                toResolve.add(link)
            } else if(info.packages.contains(dri.packageName)) {
                return link.url.toExternalForm() + getLink(dri, info)
            }
        }
        // Not in cache, resolve packageLists
        while (toResolve.isNotEmpty()){
            val link = toResolve.first().also { toResolve.remove(it) }
            val locationInfo = loadPackageList(link.packageListUrl)
            if(locationInfo.packages.contains(dri.packageName)) {
                return link.url.toExternalForm() + getLink(dri, locationInfo)
            }
        }
        return ""
    }

    private fun getLink(dri: DRI, locationInfo: LocationInfo): String = when(locationInfo.format) {
        "javadoc" ->  dri.toJavadocLocation(8)
        "kotlin-website-html", "html" -> locationInfo.locations[dri.packageName + "." + dri.classNames] ?: dri.toDokkaLocation(".html")
        "markdown" -> locationInfo.locations[dri.packageName + "." + dri.classNames] ?: dri.toDokkaLocation(".md")
        // TODO: rework this
        else -> throw RuntimeException("Unrecognized format")
    }


    private fun loadPackageList(url: URL): LocationInfo {
        val packageListStream = url.doOpenConnectionToReadContent().getInputStream()
        val (params, packages) =
            packageListStream
                .bufferedReader()
                .useLines { lines -> lines.partition { it.startsWith(DOKKA_PARAM_PREFIX) } }

        val paramsMap = params.asSequence()
            .map { it.removePrefix(DOKKA_PARAM_PREFIX).split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        val format = paramsMap["format"]?.singleOrNull() ?: "javadoc"

        val locations = paramsMap["location"].orEmpty()
            .map { it.split("\u001f", limit = 2) }
            .map { (key, value) -> key to value }
            .toMap()

        val info = LocationInfo(format, packages.toSet(), locations)
        cache[url] = info
        return info
    }

    private fun URL.doOpenConnectionToReadContent(timeout: Int = 10000, redirectsAllowed: Int = 16): URLConnection {
        val connection = this.openConnection().apply {
            connectTimeout = timeout
            readTimeout = timeout
        }

        when (connection) {
            is HttpURLConnection -> {
                return when (connection.responseCode) {
                    in 200..299 -> {
                        connection
                    }
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER -> {
                        if (redirectsAllowed > 0) {
                            val newUrl = connection.getHeaderField("Location")
                            URL(newUrl).doOpenConnectionToReadContent(timeout, redirectsAllowed - 1)
                        } else {
                            throw RuntimeException("Too many redirects")
                        }
                    }
                    else -> {
                        throw RuntimeException("Unhandled http code: ${connection.responseCode}")
                    }
                }
            }
            else -> return connection
        }
    }
    data class LocationInfo(val format: String, val packages: Set<String>, val locations: Map<String, String>)

}
