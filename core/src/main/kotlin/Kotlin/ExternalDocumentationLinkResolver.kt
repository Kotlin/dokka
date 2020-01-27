package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.io.*
import org.jetbrains.dokka.Formats.FileGeneratorBasedFormatDescriptor
import org.jetbrains.dokka.Formats.FormatDescriptor
import org.jetbrains.dokka.Utilities.ServiceLocator
import org.jetbrains.dokka.Utilities.lookup
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import javax.inject.Named
import kotlin.reflect.full.findAnnotation

fun ByteArray.toHexString() = this.joinToString(separator = "") { "%02x".format(it) }

typealias PackageFqNameToLocation = MutableMap<FqName, PackageListProvider.ExternalDocumentationRoot>

@Singleton
class PackageListProvider @Inject constructor(
    val configuration: DokkaConfiguration,
    val logger: DokkaLogger
) {
    val storage = mutableMapOf<DokkaConfiguration.ExternalDocumentationLink, PackageFqNameToLocation>()

    val cacheDir: Path? = when {
        configuration.cacheRoot == "default" -> Paths.get(System.getProperty("user.home"), ".cache", "dokka")
        configuration.cacheRoot != null -> Paths.get(configuration.cacheRoot)
        else -> null
    }?.resolve("packageListCache")?.apply { toFile().mkdirs() }

    val cachedProtocols = setOf("http", "https", "ftp")

    init {
        for (conf in configuration.passesConfigurations) {
            for (link in conf.externalDocumentationLinks) {
                if (link in storage) {
                    continue
                }

                try {
                    loadPackageList(link)
                } catch (e: Exception) {
                    throw RuntimeException("Exception while loading package-list from $link", e)
                }
            }

        }
    }



    fun URL.doOpenConnectionToReadContent(timeout: Int = 10000, redirectsAllowed: Int = 16): URLConnection {
        val connection = this.openConnection()
        connection.connectTimeout = timeout
        connection.readTimeout = timeout

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

    fun loadPackageList(link: DokkaConfiguration.ExternalDocumentationLink) {

        val packageListUrl = link.packageListUrl
        val needsCache = packageListUrl.protocol in cachedProtocols

        val packageListStream = if (cacheDir != null && needsCache) {
            val packageListLink = packageListUrl.toExternalForm()

            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(packageListLink.toByteArray(Charsets.UTF_8)).toHexString()
            val cacheEntry = cacheDir.resolve(hash).toFile()

            if (cacheEntry.exists()) {
                try {
                    val connection = packageListUrl.doOpenConnectionToReadContent()
                    val originModifiedDate = connection.date
                    val cacheDate = cacheEntry.lastModified()
                    if (originModifiedDate > cacheDate || originModifiedDate == 0L) {
                        if (originModifiedDate == 0L)
                            logger.warn("No date header for $packageListUrl, downloading anyway")
                        else
                            logger.info("Renewing package-list from $packageListUrl")
                        connection.getInputStream().copyTo(cacheEntry.outputStream())
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update package-list cache for $link")
                    val baos = ByteArrayOutputStream()
                    PrintWriter(baos).use {
                        e.printStackTrace(it)
                    }
                    baos.flush()
                    logger.error(baos.toString())
                }
            } else {
                logger.info("Downloading package-list from $packageListUrl")
                packageListUrl.openStream().copyTo(cacheEntry.outputStream())
            }
            cacheEntry.inputStream()
        } else {
            packageListUrl.doOpenConnectionToReadContent().getInputStream()
        }

        val (params, packages) =
                packageListStream
                    .bufferedReader()
                    .useLines { lines -> lines.partition { it.startsWith(ExternalDocumentationLinkResolver.DOKKA_PARAM_PREFIX) } }

        val paramsMap = params.asSequence()
            .map { it.removePrefix(ExternalDocumentationLinkResolver.DOKKA_PARAM_PREFIX).split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        val format = paramsMap["format"]?.singleOrNull() ?: "javadoc"

        val locations = paramsMap["location"].orEmpty()
            .map { it.split("\u001f", limit = 2) }
            .map { (key, value) -> key to value }
            .toMap()


        val defaultResolverDesc = ExternalDocumentationLinkResolver.services.getValue("dokka-default")
        val resolverDesc = ExternalDocumentationLinkResolver.services[format]
                ?: defaultResolverDesc.takeIf { format in formatsWithDefaultResolver }
                ?: defaultResolverDesc.also {
                    logger.warn("Couldn't find InboundExternalLinkResolutionService(format = `$format`) for $link, using Dokka default")
                }


        val resolverClass = javaClass.classLoader.loadClass(resolverDesc.className).kotlin

        val constructors = resolverClass.constructors

        val constructor = constructors.singleOrNull()
                ?: constructors.first { it.findAnnotation<Inject>() != null }
        val resolver = constructor.call(paramsMap) as InboundExternalLinkResolutionService

        val rootInfo = ExternalDocumentationRoot(link.url, resolver, locations)

        val packageFqNameToLocation = mutableMapOf<FqName, ExternalDocumentationRoot>()
        storage[link] = packageFqNameToLocation

        val fqNames = packages.map { FqName(it) }
        for(name in fqNames) {
            packageFqNameToLocation[name] = rootInfo
        }
    }



    class ExternalDocumentationRoot(val rootUrl: URL, val resolver: InboundExternalLinkResolutionService, val locations: Map<String, String>) {
        override fun toString(): String = rootUrl.toString()
    }

    companion object {
        private val formatsWithDefaultResolver =
            ServiceLocator
                .allServices("format")
                .filter {
                    val desc = ServiceLocator.lookup<FormatDescriptor>(it) as? FileGeneratorBasedFormatDescriptor
                    desc?.generatorServiceClass == FileGenerator::class
                }.map { it.name }
                .toSet()

    }

}

class ExternalDocumentationLinkResolver @Inject constructor(
        val configuration: DokkaConfiguration,
        val passConfiguration: DokkaConfiguration.PassConfiguration,
        @Named("libraryResolutionFacade") val libraryResolutionFacade: DokkaResolutionFacade,
        val logger: DokkaLogger,
        val packageListProvider: PackageListProvider
) {

    val formats = mutableMapOf<String, InboundExternalLinkResolutionService>()
    val packageFqNameToLocation = mutableMapOf<FqName, PackageListProvider.ExternalDocumentationRoot>()

    init {
        val fqNameToLocationMaps = passConfiguration.externalDocumentationLinks
            .mapNotNull { packageListProvider.storage[it] }

        for(map in fqNameToLocationMaps) {
           packageFqNameToLocation.putAll(map)
        }
    }

    fun buildExternalDocumentationLink(element: PsiElement): String? {
        return element.extractDescriptor(libraryResolutionFacade)?.let {
            buildExternalDocumentationLink(it)
        }
    }

    fun buildExternalDocumentationLink(symbol: DeclarationDescriptor): String? {
        val packageFqName: FqName =
                when (symbol) {
                    is PackageFragmentDescriptor -> symbol.fqName
                    is DeclarationDescriptorNonRoot -> symbol.parents.firstOrNull { it is PackageFragmentDescriptor }?.fqNameSafe ?: return null
                    else -> return null
                }

        val externalLocation = packageFqNameToLocation[packageFqName] ?: return null

        val path = externalLocation.locations[symbol.signature()] ?:
                externalLocation.resolver.getPath(symbol) ?: return null

        return URL(externalLocation.rootUrl, path).toExternalForm()
    }

    companion object {
        const val DOKKA_PARAM_PREFIX = "\$dokka."
        val services = ServiceLocator.allServices("inbound-link-resolver").associateBy { it.name }
    }
}


interface InboundExternalLinkResolutionService {
    fun getPath(symbol: DeclarationDescriptor): String?

    class Javadoc(paramsMap: Map<String, List<String>>) : InboundExternalLinkResolutionService {
        override fun getPath(symbol: DeclarationDescriptor): String? {
            if (symbol is EnumEntrySyntheticClassDescriptor) {
                return getPath(symbol.containingDeclaration)?.let { it + "#" + symbol.name.asString() }
            } else if (symbol is JavaClassDescriptor) {
                return DescriptorUtils.getFqName(symbol).asString().replace(".", "/") + ".html"
            } else if (symbol is JavaCallableMemberDescriptor) {
                val containingClass = symbol.containingDeclaration as? JavaClassDescriptor ?: return null
                val containingClassLink = getPath(containingClass)
                if (containingClassLink != null) {
                    if (symbol is JavaMethodDescriptor || symbol is JavaClassConstructorDescriptor) {
                        val psi = symbol.sourcePsi() as? PsiMethod
                        if (psi != null) {
                            val params = psi.parameterList.parameters.joinToString { it.type.canonicalText }
                            return containingClassLink + "#" + symbol.name + "(" + params + ")"
                        }
                    } else if (symbol is JavaPropertyDescriptor) {
                        return "$containingClassLink#${symbol.name}"
                    }
                }
            }
            // TODO Kotlin javadoc
            return null
        }
    }

    class Dokka(val paramsMap: Map<String, List<String>>) : InboundExternalLinkResolutionService {
        val extension = paramsMap["linkExtension"]?.singleOrNull() ?: error("linkExtension not provided for Dokka resolver")

        override fun getPath(symbol: DeclarationDescriptor): String? {
            val leafElement = when (symbol) {
                is CallableDescriptor, is TypeAliasDescriptor -> true
                else -> false
            }
            val path = getPathWithoutExtension(symbol)
            if (leafElement) return "$path.$extension"
            else return "$path/index.$extension"
        }

        private fun getPathWithoutExtension(symbol: DeclarationDescriptor): String {
            return when {
                symbol.containingDeclaration == null -> identifierToFilename(symbol.name.asString())
                symbol is PackageFragmentDescriptor -> identifierToFilename(symbol.fqName.asString())
                else -> getPathWithoutExtension(symbol.containingDeclaration!!) + '/' + identifierToFilename(symbol.name.asString())
            }
        }

    }
}

