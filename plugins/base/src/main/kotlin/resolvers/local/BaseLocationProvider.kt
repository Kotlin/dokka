package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.ContentSourceSet
import org.jetbrains.dokka.model.sourceSetIDs
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class BaseLocationProvider(protected val dokkaContext: DokkaContext) : LocationProvider {

    protected val externalLocationProviderFactories =
        dokkaContext.plugin<DokkaBase>().query { externalLocationProviderFactory }
    private val cache: MutableMap<URL, DefaultLocationProvider.LocationInfo> = mutableMapOf()
    private val lock = ReentrantReadWriteLock()

    protected fun getExternalLocation(
        dri: DRI,
        sourceSets: Set<ContentSourceSet>
    ): String {
        val jdkToExternalDocumentationLinks = dokkaContext.configuration.sourceSets
            .filter { sourceSet -> sourceSet.sourceSetID in sourceSets.sourceSetIDs }
            .groupBy({ it.jdkVersion }, { it.externalDocumentationLinks })
            .map { it.key to it.value.flatten().distinct() }.toMap()

        val toResolve: MutableMap<Int, MutableList<DokkaConfiguration.ExternalDocumentationLink>> = mutableMapOf()
        for ((jdk, links) in jdkToExternalDocumentationLinks) {
            for (link in links) {
                val info = lock.read { cache[link.packageListUrl] }
                if (info == null) {
                    toResolve.getOrPut(jdk) { mutableListOf() }.add(link)
                } else if (info.packages.contains(dri.packageName)) {
                    return link.url.toExternalForm() + getLink(dri, info)
                }
            }
        }
        // Not in cache, resolve packageLists
        for ((jdk, links) in toResolve) {
            for (link in links) {
                if (dokkaContext.configuration.offlineMode && link.packageListUrl.protocol.toLowerCase() != "file")
                    continue
                val locationInfo =
                    loadPackageList(jdk, link.packageListUrl)
                if (locationInfo.packages.contains(dri.packageName)) {
                    return link.url.toExternalForm() + getLink(dri, locationInfo)
                }
            }
            toResolve.remove(jdk)
        }
        return ""
    }

    private fun getLink(dri: DRI, locationInfo: DefaultLocationProvider.LocationInfo): String =
        locationInfo.locations[dri.packageName + "." + dri.classNames]
            ?: // Not sure if it can be here, previously it shadowed only kotlin/dokka related sources, here it shadows both dokka/javadoc, cause I cannot distinguish what LocationProvider has been hypothetically chosen
            if (locationInfo.externalLocationProvider != null)
                with(locationInfo.externalLocationProvider) {
                    dri.toLocation()
                }
            else
                throw IllegalStateException("Have not found any convenient ExternalLocationProvider for $dri DRI!")

    private fun loadPackageList(jdk: Int, url: URL): DefaultLocationProvider.LocationInfo = lock.write {
        val packageListStream = url.doOpenConnectionToReadContent().getInputStream()
        val (params, packages) =
            packageListStream
                .bufferedReader()
                .useLines { lines -> lines.partition { it.startsWith(DOKKA_PARAM_PREFIX) } }

        val paramsMap = params.asSequence()
            .map { it.removePrefix(DOKKA_PARAM_PREFIX).split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        val format = paramsMap["format"]?.singleOrNull() ?: when {
            jdk < 8 -> "javadoc1" // Covers JDK 1 - 7
            jdk < 10 -> "javadoc8" // Covers JDK 8 - 9
            else -> "javadoc10" // Covers JDK 10+
        }

        val locations = paramsMap["location"].orEmpty()
            .map { it.split("\u001f", limit = 2) }
            .map { (key, value) -> key to value }
            .toMap()

        val externalLocationProvider =
            externalLocationProviderFactories.asSequence().map { it.getExternalLocationProvider(format) }
                .filterNotNull().take(1).firstOrNull()

        val info = DefaultLocationProvider.LocationInfo(
            externalLocationProvider,
            packages.toSet(),
            locations
        )
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

    companion object {
        const val DOKKA_PARAM_PREFIX = "\$dokka."
    }

}
