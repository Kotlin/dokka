package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.tasks.Classpath
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
    @Classpath @Optional override var classpath: List<String> = emptyList()
    @Input override var moduleName: String = ""
    @Input override var sourceRoots: MutableList<SourceRoot> = mutableListOf()
    @Input override var samples: List<String> = emptyList()
    @Input override var includes: List<String> = emptyList()
    @Input override var includeNonPublic: Boolean = false
    @Input override var includeRootPackage: Boolean = false
    @Input override var reportUndocumented: Boolean = false
    @Input override var skipEmptyPackages: Boolean = true
    @Input override var skipDeprecated: Boolean = false
    @Input override var jdkVersion: Int = 6
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
    @Input @Transient var androidVariants: List<String> = emptyList()

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
        val builder = ConfigureUtil.configure(c, GradleExternalDocumentationLinkImpl.Builder())
        externalDocumentationLinks.add(builder.build())
    }

    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkImpl.Builder>) {
        val builder = GradleExternalDocumentationLinkImpl.Builder()
        action.execute(builder)
        externalDocumentationLinks.add(builder.build())
    }
}

class GradleSourceLinkDefinitionImpl : SourceLinkDefinition, Serializable {
    override var path: String = ""
    override var url: String = ""
    override var lineSuffix: String? = null
}

class GradleExternalDocumentationLinkImpl(
    override val url: URL,
    override val packageListUrl: URL
): ExternalDocumentationLink, Serializable {
    open class Builder(open var url: URL? = null,
                       open var packageListUrl: URL? = null) {

        constructor(root: String, packageList: String? = null) : this(URL(root), packageList?.let { URL(it) })

        fun build(): ExternalDocumentationLink =
            if (packageListUrl != null && url != null)
                GradleExternalDocumentationLinkImpl(url!!, packageListUrl!!)
            else if (url != null)
                GradleExternalDocumentationLinkImpl(url!!, URL(url!!, "package-list"))
            else
                throw IllegalArgumentException("url or url && packageListUrl must not be null for external documentation link")
    }
}

class GradleDokkaConfigurationImpl: DokkaConfiguration {
    override var outputDir: String = ""
    override var format: String = "html"
    override var generateIndexPages: Boolean = false
    override var cacheRoot: String? = null
    override var impliedPlatforms: List<String> = emptyList()
    override var passesConfigurations: List<GradlePassConfigurationImpl> = emptyList()
}

class GradlePackageOptionsImpl: PackageOptions, Serializable {
    override var prefix: String = ""
    override var includeNonPublic: Boolean = false
    override var reportUndocumented: Boolean = false
    override var skipDeprecated: Boolean = false
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
