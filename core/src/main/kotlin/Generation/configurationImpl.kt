package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.SourceLinkDefinition
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import java.io.File


data class SourceLinkDefinitionImpl(override val path: String,
                                    override val url: String,
                                    override val lineSuffix: String?) : SourceLinkDefinition {
    companion object {
        fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinition {
            val (path, urlAndLine) = srcLink.split('=')
            return SourceLinkDefinitionImpl(File(path).absolutePath,
                    urlAndLine.substringBefore("#"),
                    urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#" + it })
        }
    }
}

class SourceRootImpl(path: String, override val platforms: List<String> = emptyList(),
                     override val analysisPlatform: Platform = Platform.DEFAULT) : SourceRoot {
    override val path: String = File(path).absolutePath

    companion object {
        fun parseSourceRoot(sourceRoot: String): SourceRoot {
            val components = sourceRoot.split("::", limit = 2)

            val platform = if (components.size == 1) {
                Platform.DEFAULT
            } else {
                Platform.fromString(components[0])
            }

            return SourceRootImpl(components.last(), emptyList(), platform)
        }
    }
}

data class PackageOptionsImpl(override val prefix: String,
                              override val includeNonPublic: Boolean = false,
                              override val reportUndocumented: Boolean = true,
                              override val skipDeprecated: Boolean = false,
                              override val suppress: Boolean = false) : DokkaConfiguration.PackageOptions

data class DokkaConfigurationImpl(
    override val outputDir: String = "",
    override val format: String = "html",
    override val generateIndexPages: Boolean = false,
    override val cacheRoot: String? = null,
    override val impliedPlatforms: List<String> = listOf(),
    override val passesConfigurations: List<DokkaConfiguration.PassConfiguration> = listOf()
) : DokkaConfiguration

class PassConfigurationImpl (
    override val classpath: List<String> = listOf(),
    override val moduleName: String = "",
    override val sourceRoots: List<SourceRoot> = listOf(),
    override val samples: List<String> = listOf(),
    override val includes: List<String> = listOf(),
    override val includeNonPublic: Boolean = false,
    override val includeRootPackage: Boolean = false,
    override val reportUndocumented: Boolean = false,
    override val skipEmptyPackages: Boolean = false,
    override val skipDeprecated: Boolean = false,
    override val jdkVersion: Int = 6,
    override val sourceLinks: List<SourceLinkDefinition> = listOf(),
    override val perPackageOptions: List<DokkaConfiguration.PackageOptions> = listOf(),
    externalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink> = listOf(),
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val noStdlibLink: Boolean = false,
    override val noJdkLink: Boolean = false,
    override val suppressedFiles: List<String> = listOf(),
    override val collectInheritedExtensionsFromLibraries: Boolean = false,
    override val analysisPlatform: Platform = Platform.DEFAULT,
    override val targets: List<String> = listOf()
): DokkaConfiguration.PassConfiguration {
    private val defaultLinks = run {
        val links = mutableListOf<DokkaConfiguration.ExternalDocumentationLink>()
        if (!noJdkLink)
            links += DokkaConfiguration.ExternalDocumentationLink.Builder("http://docs.oracle.com/javase/$jdkVersion/docs/api/").build()

        if (!noStdlibLink)
            links += DokkaConfiguration.ExternalDocumentationLink.Builder("https://kotlinlang.org/api/latest/jvm/stdlib/").build()
        links
    }
    override val externalDocumentationLinks = defaultLinks + externalDocumentationLinks
}

