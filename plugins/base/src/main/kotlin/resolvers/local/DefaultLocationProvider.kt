package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import java.lang.IllegalStateException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.*

private const val PAGE_WITH_CHILDREN_SUFFIX = "index"
private const val DOKKA_PARAM_PREFIX = "\$dokka."

open class DefaultLocationProvider(
    protected val pageGraphRoot: RootPageNode,
    protected val dokkaContext: DokkaContext
) : LocationProvider {
    protected val extension = ".html"

    protected val externalLocationProviderFactories =
        dokkaContext.plugin<DokkaBase>().query { externalLocationProviderFactory }


    protected val pagesIndex: Map<DRI, ContentPage> = pageGraphRoot.asSequence().filterIsInstance<ContentPage>()
        .map { it.dri.map { dri -> dri to it } }.flatten()
        .groupingBy { it.first }
        .aggregate { dri, _, (_, page), first ->
            if (first) page else throw AssertionError("Multiple pages associated with dri: $dri")
        }

    protected val pathsIndex: Map<PageNode, List<String>> = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String>) {
            val newPrefix = prefix + page.pathName
            put(page, newPrefix)
            page.children.forEach { registerPath(it, newPrefix) }
        }
        put(pageGraphRoot, emptyList())
        pageGraphRoot.children.forEach { registerPath(it, emptyList()) }
    }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        pathTo(node, context) + if (!skipExtension) extension else ""

    override fun resolve(dri: DRI, platforms: List<PlatformData>, context: PageNode?): String =
        pagesIndex[dri]?.let { resolve(it, context) } ?:
        // Not found in PageGraph, that means it's an external link
        getLocation(dri,
            this.dokkaContext.configuration.passesConfigurations
                .filter { passConfig ->
                    platforms.toSet()
                        .contains(PlatformData(passConfig.moduleName, passConfig.analysisPlatform, passConfig.targets))
                } // TODO: change targets to something better?
                .groupBy ({ it.jdkVersion }, { it.externalDocumentationLinks } )
                .map { it.key to it.value.flatten().distinct() }.toMap()
        )

    override fun resolveRoot(node: PageNode): String =
        pathTo(pageGraphRoot, node).removeSuffix(PAGE_WITH_CHILDREN_SUFFIX)

    override fun ancestors(node: PageNode): List<PageNode> =
        generateSequence(node) { it.parent() }.toList()

    protected open fun pathTo(node: PageNode, context: PageNode?): String {
        fun pathFor(page: PageNode) = pathsIndex[page] ?: throw AssertionError(
            "${page::class.simpleName}(${page.name}) does not belong to current page graph so it is impossible to compute its path"
        )

        val contextNode =
            if (context?.children?.isEmpty() == true && context.parent() != null) context.parent() else context
        val nodePath = pathFor(node)
        val contextPath = contextNode?.let { pathFor(it) }.orEmpty()

        val commonPathElements = nodePath.asSequence().zip(contextPath.asSequence())
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) +
                if (node.children.isNotEmpty()) listOf(PAGE_WITH_CHILDREN_SUFFIX) else emptyList()).joinToString("/")
    }

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]




    private val cache: MutableMap<URL, LocationInfo> = mutableMapOf()

    private fun getLocation(dri: DRI, jdkToExternalDocumentationLinks: Map<Int, List<DokkaConfiguration.ExternalDocumentationLink>>): String {
        val toResolve: MutableMap<Int, MutableList<DokkaConfiguration.ExternalDocumentationLink>> = mutableMapOf()
        for((jdk, links) in jdkToExternalDocumentationLinks) {
            for(link in links) {
                val info = cache[link.packageListUrl]
                if(info == null) {
                    toResolve.getOrPut(jdk) { mutableListOf(link) }.add(link)
                } else if(info.packages.contains(dri.packageName)) {
                    return link.url.toExternalForm() + getLink(
                        dri,
                        info
                    )
                }
            }
        }
        // Not in cache, resolve packageLists
        for ((jdk, links) in toResolve) {
            for(link in links) {
                val locationInfo =
                    loadPackageList(
                        jdk,
                        link.packageListUrl
                    )
                if(locationInfo.packages.contains(dri.packageName)) {
                    return link.url.toExternalForm() + getLink(
                        dri,
                        locationInfo
                    )
                }
            }
            toResolve.remove(jdk)
        }
        return ""
    }

    private fun getLink(dri: DRI, locationInfo: LocationInfo): String  =
        locationInfo.locations[dri.packageName + "." + dri.classNames] ?: // Not sure if it can be here, previously it shadowed only kotlin/dokka related sources, here it shadows both dokka/javadoc, cause I cannot distinguish what LocationProvider has been hypothetically chosen
        if(locationInfo.externalLocationProvider != null)
            with(locationInfo.externalLocationProvider) {
                dri.toLocation()
            }
        else
            throw IllegalStateException("Have not found any convenient ExternalLocationProvider for $dri DRI!")

    private fun loadPackageList(jdk: Int, url: URL): LocationInfo {
        val packageListStream = url.doOpenConnectionToReadContent().getInputStream()
        val (params, packages) =
            packageListStream
                .bufferedReader()
                .useLines { lines -> lines.partition { it.startsWith(DOKKA_PARAM_PREFIX) } }

        val paramsMap = params.asSequence()
            .map { it.removePrefix(DOKKA_PARAM_PREFIX).split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        val format = paramsMap["format"]?.singleOrNull() ?:
            when {
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

        val info = LocationInfo(
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

    data class LocationInfo(val externalLocationProvider: ExternalLocationProvider?, val packages: Set<String>, val locations: Map<String, String>)
}

private val reservedFilenames = setOf("index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out")

internal fun identifierToFilename(name: String): String {
    if (name.isEmpty()) return "--root--"
    val escaped = name.replace('<', '-').replace('>', '-')
    val lowercase = escaped.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    return if (lowercase in reservedFilenames) "--$lowercase--" else lowercase
}

private val PageNode.pathName: String
    get() = if (this is PackagePageNode) name else identifierToFilename(
        name
    )
