/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import java.io.File
import java.net.URL

public data class DokkaConfigurationImpl(
    override val moduleName: String = DokkaDefaults.moduleName,
    override val moduleVersion: String? = DokkaDefaults.moduleVersion,
    override val outputDir: File = DokkaDefaults.outputDir,
    override val cacheRoot: File? = DokkaDefaults.cacheRoot,
    override val offlineMode: Boolean = DokkaDefaults.offlineMode,
    override val sourceSets: List<DokkaSourceSetImpl> = emptyList(),
    override val pluginsClasspath: List<File> = emptyList(),
    override val pluginsConfiguration: List<PluginConfigurationImpl> = DokkaDefaults.pluginsConfiguration,
    override val modules: List<DokkaModuleDescriptionImpl> = emptyList(),
    override val failOnWarning: Boolean = DokkaDefaults.failOnWarning,
    override val delayTemplateSubstitution: Boolean = false,
    override val suppressObviousFunctions: Boolean = DokkaDefaults.suppressObviousFunctions,
    override val includes: Set<File> = emptySet(),
    override val suppressInheritedMembers: Boolean = DokkaDefaults.suppressInheritedMembers,
    override val finalizeCoroutines: Boolean = true,
) : DokkaConfiguration

public data class PluginConfigurationImpl(
    override val fqPluginName: String,
    override val serializationFormat: DokkaConfiguration.SerializationFormat,
    override val values: String
) : DokkaConfiguration.PluginConfiguration


public data class DokkaSourceSetImpl(
    override val displayName: String = DokkaDefaults.sourceSetDisplayName,
    override val sourceSetID: DokkaSourceSetID,
    override val classpath: List<File> = emptyList(),
    override val sourceRoots: Set<File> = emptySet(),
    override val dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    override val samples: Set<File> = emptySet(),
    override val includes: Set<File> = emptySet(),
    @Deprecated("Use [documentedVisibilities] property for a more flexible control over documented visibilities")
    override val includeNonPublic: Boolean = DokkaDefaults.includeNonPublic,
    override val reportUndocumented: Boolean = DokkaDefaults.reportUndocumented,
    override val skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages,
    override val skipDeprecated: Boolean = DokkaDefaults.skipDeprecated,
    override val jdkVersion: Int = DokkaDefaults.jdkVersion,
    override val sourceLinks: Set<SourceLinkDefinitionImpl> = mutableSetOf(),
    override val perPackageOptions: List<PackageOptionsImpl> = mutableListOf(),
    override val externalDocumentationLinks: Set<ExternalDocumentationLinkImpl> = mutableSetOf(),
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val noStdlibLink: Boolean = DokkaDefaults.noStdlibLink,
    override val noJdkLink: Boolean = DokkaDefaults.noJdkLink,
    override val suppressedFiles: Set<File> = emptySet(),
    override val analysisPlatform: Platform = DokkaDefaults.analysisPlatform,
    override val documentedVisibilities: Set<DokkaConfiguration.Visibility> = DokkaDefaults.documentedVisibilities,
) : DokkaSourceSet

public data class DokkaModuleDescriptionImpl(
    override val name: String,
    override val relativePathToOutputDirectory: File,
    override val includes: Set<File>,
    override val sourceOutputDirectory: File
) : DokkaConfiguration.DokkaModuleDescription

public data class SourceLinkDefinitionImpl(
    override val localDirectory: String,
    override val remoteUrl: URL,
    override val remoteLineSuffix: String?,
) : DokkaConfiguration.SourceLinkDefinition {

    public companion object {
        public fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinitionImpl {
            val (path, urlAndLine) = srcLink.split('=')
            return SourceLinkDefinitionImpl(
                localDirectory = File(path).canonicalPath,
                remoteUrl = URL(urlAndLine.substringBefore("#")),
                remoteLineSuffix = urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#$it" })
        }
    }
}

public data class PackageOptionsImpl(
    override val matchingRegex: String,
    @Deprecated("Use [documentedVisibilities] property for a more flexible control over documented visibilities")
    override val includeNonPublic: Boolean,
    override val reportUndocumented: Boolean?,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean,
    override val documentedVisibilities: Set<DokkaConfiguration.Visibility>, // TODO add default to DokkaDefaults.documentedVisibilities
) : DokkaConfiguration.PackageOptions


public data class ExternalDocumentationLinkImpl(
    override val url: URL,
    override val packageListUrl: URL,
) : DokkaConfiguration.ExternalDocumentationLink
