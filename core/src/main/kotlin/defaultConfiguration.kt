package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaSourceSet.*
import java.io.File
import java.net.URL

data class DokkaModuleConfigurationImpl(
    override val moduleName: String = DokkaDefaults.moduleName,
    override val outputDir: File = DokkaDefaults.outputDir,
    override val cacheRoot: File? = DokkaDefaults.cacheRoot,
    override val offlineMode: Boolean = DokkaDefaults.offlineMode,
    override val sourceSets: List<DokkaSourceSetImpl> = emptyList(),
    override val pluginsClasspath: List<File> = emptyList(),
    override val pluginsConfiguration: Map<String, String> = emptyMap(),
    override val failOnWarning: Boolean = DokkaDefaults.failOnWarning
) : DokkaModuleConfiguration

data class DokkaMultiModuleConfigurationImpl(
    override val projectName: String = DokkaDefaults.projectName,
    override val outputDir: File = DokkaDefaults.outputDir,
    override val cacheRoot: File? = DokkaDefaults.cacheRoot,
    override val offlineMode: Boolean = DokkaDefaults.offlineMode,
    override val pluginsClasspath: List<File> = emptyList(),
    override val pluginsConfiguration: Map<String, String> = emptyMap(),
    override val failOnWarning: Boolean = DokkaDefaults.failOnWarning,
    override val modules: List<DokkaModuleConfiguration> = emptyList()
) : DokkaMultiModuleConfiguration

data class DokkaSourceSetImpl(
    override val displayName: String = DokkaDefaults.sourceSetDisplayName,
    override val sourceSetID: DokkaSourceSetID,
    override val classpath: List<File> = emptyList(),
    override val sourceRoots: Set<File> = emptySet(),
    override val dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    override val samples: Set<File> = emptySet(),
    override val includes: Set<File> = emptySet(),
    override val includeNonPublic: Boolean = DokkaDefaults.includeNonPublic,
    override val reportUndocumented: Boolean = DokkaDefaults.reportUndocumented,
    override val skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages,
    override val skipDeprecated: Boolean = DokkaDefaults.skipDeprecated,
    override val jdkVersion: Int = DokkaDefaults.jdkVersion,
    override val sourceLinks: Set<SourceLinkDefinitionImpl> = emptySet(),
    override val perPackageOptions: List<PackageOptionsImpl> = emptyList(),
    override val externalDocumentationLinks: Set<ExternalDocumentationLinkImpl> = emptySet(),
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val noStdlibLink: Boolean = DokkaDefaults.noStdlibLink,
    override val noJdkLink: Boolean = DokkaDefaults.noJdkLink,
    override val suppressedFiles: Set<File> = emptySet(),
    override val analysisPlatform: Platform = DokkaDefaults.analysisPlatform
) : DokkaSourceSet

data class SourceLinkDefinitionImpl(
    override val localDirectory: String,
    override val remoteUrl: URL,
    override val remoteLineSuffix: String?
) : SourceLinkDefinition {
    companion object {
        fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinitionImpl {
            val (path, urlAndLine) = srcLink.split('=')
            return SourceLinkDefinitionImpl(
                File(path).canonicalPath,
                URL(urlAndLine.substringBefore("#")),
                urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#$it" })
        }
    }
}

data class PackageOptionsImpl(
    override val prefix: String,
    override val includeNonPublic: Boolean,
    override val reportUndocumented: Boolean?,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean
) : PackageOptions


data class ExternalDocumentationLinkImpl(
    override val url: URL,
    override val packageListUrl: URL
) : ExternalDocumentationLink
