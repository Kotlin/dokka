package org.jetbrains.dokka

import java.io.File
import java.net.URL

enum class Platform(val key: String) {
    jvm("jvm"),
    js("js"),
    native("native"),
    common("common");

    companion object {
        val DEFAULT = jvm

        fun fromString(key: String): Platform {
            return when (key.toLowerCase()) {
                jvm.key -> jvm
                js.key -> js
                native.key -> native
                common.key -> common
                else -> throw IllegalArgumentException("Unrecognized platform: $key")
            }
        }
    }
}

interface DokkaConfiguration {
    val outputDir: String
    val format: String
    val cacheRoot: String?
    val offlineMode: Boolean
    val passesConfigurations: List<PassConfiguration>
    val modules: List<DokkaModuleDescription>
    val pluginsClasspath: List<File>
    val pluginsConfiguration: Map<String, String>
    val failOnWarning: Boolean

    interface PassConfiguration {
        val moduleName: String
        val displayName: String
        val sourceSetID: String
        val classpath: List<String>
        val sourceRoots: List<SourceRoot>
        val dependentSourceSets: List<String>
        val samples: List<String>
        val includes: List<String>
        val includeNonPublic: Boolean
        val includeRootPackage: Boolean
        val reportUndocumented: Boolean
        val skipEmptyPackages: Boolean
        val skipDeprecated: Boolean
        val jdkVersion: Int
        val sourceLinks: List<SourceLinkDefinition>
        val perPackageOptions: List<PackageOptions>
        val externalDocumentationLinks: List<ExternalDocumentationLink>
        val languageVersion: String?
        val apiVersion: String?
        val noStdlibLink: Boolean
        val noJdkLink: Boolean
        val suppressedFiles: List<String>
        val analysisPlatform: Platform
    }

    interface SourceRoot {
        val path: String
    }

    interface SourceLinkDefinition {
        val path: String
        val url: String
        val lineSuffix: String?
    }

    interface DokkaModuleDescription {
        val name: String
        val path: String
        val docFile: String
    }

    interface PackageOptions {
        val prefix: String
        val includeNonPublic: Boolean
        val reportUndocumented: Boolean?
        val skipDeprecated: Boolean
        val suppress: Boolean
    }

    interface ExternalDocumentationLink {
        val url: URL
        val packageListUrl: URL

        open class Builder(open var url: URL? = null,
                           open var packageListUrl: URL? = null) {

            constructor(root: String, packageList: String? = null) : this(URL(root), packageList?.let { URL(it) })

            fun build(): ExternalDocumentationLink =
                if (packageListUrl != null && url != null)
                    ExternalDocumentationLinkImpl(url!!, packageListUrl!!)
                else if (url != null)
                    ExternalDocumentationLinkImpl(url!!, URL(url!!, "package-list"))
                else
                    throw IllegalArgumentException("url or url && packageListUrl must not be null for external documentation link")
        }
    }
}


