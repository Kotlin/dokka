package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.util.ConfigureUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.*
import org.jetbrains.dokka.Platform
import java.io.File
import java.io.Serializable
import java.net.URL
import java.util.concurrent.Callable
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
    @Input @Optional override var classpath: List<String> = emptyList()
    @Input override var moduleName: String = ""
    @Input override var sourceSetName: String = ""
    @Input override var sourceRoots: MutableList<SourceRoot> = mutableListOf()
    @Input override var dependentSourceRoots: MutableList<SourceRoot> = mutableListOf()
    @Input override var dependentSourceSets: MutableList<String> = mutableListOf()
    @Input override var samples: List<String> = emptyList()
    @Input override var includes: List<String> = emptyList()
    @Input override var includeNonPublic: Boolean = false
    @Input override var includeRootPackage: Boolean = false
    @Input override var reportUndocumented: Boolean = false
    @Input override var skipEmptyPackages: Boolean = false
    @Input override var skipDeprecated: Boolean = false
    @Input override var jdkVersion: Int = 8
    @Input override var sourceLinks: MutableList<SourceLinkDefinition> = mutableListOf()
    @Input override var perPackageOptions: MutableList<PackageOptions> = mutableListOf()
    @Input override var externalDocumentationLinks: MutableList<ExternalDocumentationLink> = mutableListOf()
    @Input @Optional override var languageVersion: String? = null
    @Input @Optional override var apiVersion: String? = null
    @Input override var noStdlibLink: Boolean = false
    @Input override var noJdkLink: Boolean = false
    @Input var noAndroidSdkLink: Boolean = false
    @Input override var suppressedFiles: List<String> = emptyList()
    @Input override var collectInheritedExtensionsFromLibraries: Boolean = false
    @Input override var analysisPlatform: Platform = Platform.DEFAULT
    @Input @Optional var platform: String? = null
    @Input override var targets: List<String> = emptyList()
    @Input @Optional override var sinceKotlin: String? = null
    @Transient var collectKotlinTasks: (() -> List<Any?>?)? = null
    @Input @Optional @Transient var androidVariant: String? = null

    fun kotlinTasks(taskSupplier: Callable<List<Any>>) {
        collectKotlinTasks = { taskSupplier.call() }
    }

    fun kotlinTasks(closure: Closure<Any?>) {
        collectKotlinTasks = { closure.call() as? List<Any?> }
    }

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

class GradleSourceLinkDefinitionImpl : SourceLinkDefinition, Serializable {
    override var path: String = ""
    override var url: String = ""
    override var lineSuffix: String? = null
}

class GradleExternalDocumentationLinkImpl : ExternalDocumentationLink, Serializable {
    override var url: URL = URL("http://")
    override var packageListUrl: URL = URL("http://")
}

class GradleDokkaModuleDescription: DokkaModuleDescription {
    override var name: String = ""
    override var path: String = ""
    override var docFile: String = ""
}

class GradleDokkaConfigurationImpl: DokkaConfiguration {
    override var outputDir: String = ""
    override var format: String = "html"
    override var generateIndexPages: Boolean = false
    override var cacheRoot: String? = null
    override var impliedPlatforms: List<String> = emptyList()
    override var passesConfigurations: List<GradlePassConfigurationImpl> = emptyList()
    override var pluginsClasspath: List<File> = emptyList()
    override var pluginsConfiguration: Map<String, String> = mutableMapOf()
    override var modules: List<GradleDokkaModuleDescription> = emptyList()
}

class GradlePackageOptionsImpl: PackageOptions, Serializable {
    override var prefix: String = ""
    override var includeNonPublic: Boolean = false
    override var reportUndocumented: Boolean = true
    override var skipDeprecated: Boolean = true
    override var suppress: Boolean = false
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