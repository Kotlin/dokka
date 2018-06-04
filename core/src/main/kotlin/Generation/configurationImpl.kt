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

class SourceRootImpl(path: String, override val platforms: List<String> = emptyList()) : SourceRoot {
    override val path: String = File(path).absolutePath

    companion object {
        fun parseSourceRoot(sourceRoot: String): SourceRoot {
            val components = sourceRoot.split("::", limit = 2)
            return SourceRootImpl(components.last(), if (components.size == 1) listOf() else components[0].split(','))
        }
    }
}

data class PackageOptionsImpl(override val prefix: String,
                              override val includeNonPublic: Boolean = false,
                              override val reportUndocumented: Boolean = true,
                              override val skipDeprecated: Boolean = false,
                              override val suppress: Boolean = false) : DokkaConfiguration.PackageOptions

data class DokkaConfigurationImpl(
    override val moduleName: String,
    override val classpath: List<String>,
    override val sourceRoots: List<SourceRootImpl>,
    override val samples: List<String>,
    override val includes: List<String>,
    override val outputDir: String,
    override val format: String,
    override val includeNonPublic: Boolean,
    override val includeRootPackage: Boolean,
    override val reportUndocumented: Boolean,
    override val skipEmptyPackages: Boolean,
    override val skipDeprecated: Boolean,
    override val jdkVersion: Int,
    override val generateClassIndexPage: Boolean,
    override val generatePackageIndexPage: Boolean,
    override val sourceLinks: List<SourceLinkDefinitionImpl>,
    override val impliedPlatforms: List<String>,
    override val perPackageOptions: List<PackageOptionsImpl>,
    override val externalDocumentationLinks: List<ExternalDocumentationLinkImpl>,
    override val noStdlibLink: Boolean,
    override val noJdkLink: Boolean,
    override val cacheRoot: String?,
    override val suppressedFiles: List<String>,
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val collectInheritedExtensionsFromLibraries: Boolean,
    override val outlineRoot: String,
    override val dacRoot: String
) : DokkaConfiguration