package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import java.io.File


data class SourceLinkDefinitionImpl(override val path: String,
                                    override val url: String,
                                    override val lineSuffix: String?) : DokkaConfiguration.SourceLinkDefinition {
    companion object {
        fun parseSourceLinkDefinition(srcLink: String): DokkaConfiguration.SourceLinkDefinition {
            val (path, urlAndLine) = srcLink.split('=')
            return SourceLinkDefinitionImpl(
                File(path).canonicalPath,
                    urlAndLine.substringBefore("#"),
                    urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#$it" })
        }
    }
}

class SourceRootImpl(path: String) : DokkaConfiguration.SourceRoot {
    override val path: String = File(path).absolutePath

    companion object {
        fun parseSourceRoot(sourceRoot: String): DokkaConfiguration.SourceRoot = SourceRootImpl(sourceRoot)
    }
}

data class PackageOptionsImpl(override val prefix: String,
                              override val includeNonPublic: Boolean = false,
                              override val reportUndocumented: Boolean = true,
                              override val skipDeprecated: Boolean = false,
                              override val suppress: Boolean = false) : DokkaConfiguration.PackageOptions

 class DokkaConfigurationImpl(
    override val outputDir: String = "",
    override val format: String = "html",
    override val generateIndexPages: Boolean = false,
    override val cacheRoot: String? = null,
    override val impliedPlatforms: List<String> = emptyList(),
    override val passesConfigurations: List<DokkaConfiguration.PassConfiguration> = emptyList()
) : DokkaConfiguration

class PassConfigurationImpl (
    override val classpath: List<String> = emptyList(),
    override val moduleName: String = "",
    override val sourceRoots: List<DokkaConfiguration.SourceRoot> = emptyList(),
    override val samples: List<String> = emptyList(),
    override val includes: List<String> = emptyList(),
    override val includeNonPublic: Boolean = false,
    override val includeRootPackage: Boolean = false,
    override val reportUndocumented: Boolean = false,
    override val skipEmptyPackages: Boolean = false,
    override val skipDeprecated: Boolean = false,
    override val jdkVersion: Int = 6,
    override val sourceLinks: List<DokkaConfiguration.SourceLinkDefinition> = emptyList(),
    override val perPackageOptions: List<DokkaConfiguration.PackageOptions> = emptyList(),
    externalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink> = emptyList(),
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val noStdlibLink: Boolean = false,
    override val noJdkLink: Boolean = false,
    override val suppressedFiles: List<String> = emptyList(),
    override val collectInheritedExtensionsFromLibraries: Boolean = false,
    override val analysisPlatform: Platform = Platform.DEFAULT,
    override val targets: List<String> = emptyList(),
    override val sinceKotlin: String? = null
): DokkaConfiguration.PassConfiguration {
    private val defaultLinks = run {
        val links = mutableListOf<DokkaConfiguration.ExternalDocumentationLink>()
        if (!noJdkLink)
            links += DokkaConfiguration.ExternalDocumentationLink.Builder("https://docs.oracle.com/javase/$jdkVersion/docs/api/").build()

        if (!noStdlibLink)
            links += DokkaConfiguration.ExternalDocumentationLink.Builder("https://kotlinlang.org/api/latest/jvm/stdlib/").build()
        links
    }
    override val externalDocumentationLinks = defaultLinks + externalDocumentationLinks
}

