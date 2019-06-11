package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.*
import org.jetbrains.dokka.Platform
import java.io.File
import java.io.Serializable
import java.net.URL
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

class GradleSourceRootImpl: SourceRoot, Serializable {
    override var path: String = ""
        set(value) {
            field = File(value).absolutePath
        }

    override fun toString(): String = path
}

open class GradlePassConfigurationImpl(@Transient val name: String = ""): PassConfiguration {
    override var classpath: List<String> = emptyList()
    override var moduleName: String = ""
    override var sourceRoots: MutableList<SourceRoot> = mutableListOf()
    override var samples: List<String> = emptyList()
    override var includes: List<String> = emptyList()
    override var includeNonPublic: Boolean = false
    override var includeRootPackage: Boolean = false
    override var reportUndocumented: Boolean = false
    override var skipEmptyPackages: Boolean = false
    override var skipDeprecated: Boolean = false
    override var jdkVersion: Int = 6
    override var sourceLinks: MutableList<SourceLinkDefinition> = mutableListOf()
    override var perPackageOptions: MutableList<PackageOptions> = mutableListOf()
    override var externalDocumentationLinks: MutableList<ExternalDocumentationLink> = mutableListOf()
    override var languageVersion: String? = null
    override var apiVersion: String? = null
    override var noStdlibLink: Boolean = false
    override var noJdkLink: Boolean = false
    override var suppressedFiles: List<String> = emptyList()
    override var collectInheritedExtensionsFromLibraries: Boolean = false
    override var analysisPlatform: Platform = Platform.DEFAULT
    var platform: String? = null
    override var targets: List<String> = emptyList()
    override var sinceKotlin: String? = null

    fun sourceRoot(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradleSourceRootImpl())
        sourceRoots.add(configured)
    }

    fun sourceRoot(action: Action<in GradleSourceRootImpl>) {
        val sourceRoot = GradleSourceRootImpl()
        action.execute(sourceRoot)
        sourceRoots.add(sourceRoot)
    }

    fun sourceLink(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradleSourceLinkDefinitionImpl())
        sourceLinks.add(configured)
    }

    fun sourceLink(action: Action<in GradleSourceLinkDefinitionImpl>) {
        val sourceLink = GradleSourceLinkDefinitionImpl()
        action.execute(sourceLink)
        sourceLinks.add(sourceLink)
    }

    fun perPackageOption(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradlePackageOptionsImpl())
        perPackageOptions.add(configured)
    }

    fun perPackageOption(action: Action<in GradlePackageOptionsImpl>) {
        val option = GradlePackageOptionsImpl()
        action.execute(option)
        perPackageOptions.add(option)
    }

    fun externalDocumentationLink(c: Closure<Unit>) {
        val link = ConfigureUtil.configure(c, GradleExternalDocumentationLinkImpl())
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkImpl>) {
        val link = GradleExternalDocumentationLinkImpl()
        action.execute(link)
        externalDocumentationLinks.add(link)
    }
}

class GradleSourceLinkDefinitionImpl : SourceLinkDefinition {
    override var path: String = ""
    override var url: String = ""
    override var lineSuffix: String? = null
}

class GradleExternalDocumentationLinkImpl : ExternalDocumentationLink {
    override var url: URL = URL("http://")
    override var packageListUrl: URL = URL("http://")
}

class GradleDokkaConfigurationImpl: DokkaConfiguration {
    override var outputDir: String = ""
    override var format: String = "html"
    override var generateIndexPages: Boolean = false
    override var cacheRoot: String? = null
    override var impliedPlatforms: List<String> = emptyList()
    override var passesConfigurations: List<GradlePassConfigurationImpl> = emptyList()
}

class GradlePackageOptionsImpl: PackageOptions {
    override var prefix: String = ""
    override val includeNonPublic: Boolean = false
    override val reportUndocumented: Boolean = true
    override val skipDeprecated: Boolean = true
    override val suppress: Boolean = false
}

fun GradlePassConfigurationImpl.copy(): GradlePassConfigurationImpl {
    val newObj = GradlePassConfigurationImpl(this.name)
    this::class.memberProperties.forEach { field ->
        if (field is KMutableProperty<*>) {
            val value = field.getter.call(this)
            if (value is Collection<*>) {
                field.setter.call(newObj, value.toMutableList())
            } else {
                field.setter.call(newObj, field.getter.call(this))
            }
        }
    }
    return newObj
}