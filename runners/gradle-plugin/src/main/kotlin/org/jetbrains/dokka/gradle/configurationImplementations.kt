@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import com.android.build.gradle.api.AndroidSourceSet
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.util.ConfigureUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.*
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import java.io.File
import java.io.Serializable
import java.net.URL
import java.util.concurrent.Callable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import org.gradle.api.tasks.SourceSet as GradleSourceSet
import org.jetbrains.kotlin.gradle.model.SourceSet as KotlinSourceSet

class GradleSourceRootImpl : SourceRoot, Serializable {
    override var path: String = ""
        set(value) {
            field = File(value).absolutePath
        }

    override fun toString(): String = path
}

open class GradleDokkaSourceSet constructor(
    @Transient @get:Input val name: String,
    @Transient @get:Internal internal val project: Project
) : DokkaSourceSet {

    @Classpath
    @Optional
    override var classpath: List<String> = emptyList()

    @Input
    override var moduleDisplayName: String = ""

    @Input
    override var displayName: String = ""

    @get:Internal
    override val sourceSetID: DokkaSourceSetID = DokkaSourceSetID(project, name)

    @Input
    override var sourceRoots: MutableList<SourceRoot> = mutableListOf()

    @Input
    override var dependentSourceSets: MutableSet<DokkaSourceSetID> = mutableSetOf()

    @Input
    override var samples: List<String> = emptyList()

    @Input
    override var includes: List<String> = emptyList()

    @Input
    override var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic

    @Input
    override var includeRootPackage: Boolean = DokkaDefaults.includeRootPackage

    @Input
    override var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

    @Input
    override var skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages

    @Input
    override var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

    @Input
    override var jdkVersion: Int = DokkaDefaults.jdkVersion

    @Input
    override var sourceLinks: MutableList<SourceLinkDefinition> = mutableListOf()

    @Input
    override var perPackageOptions: MutableList<PackageOptions> = mutableListOf()

    @Input
    override var externalDocumentationLinks: MutableList<ExternalDocumentationLink> = mutableListOf()

    @Input
    @Optional
    override var languageVersion: String? = null

    @Input
    @Optional
    override var apiVersion: String? = null

    @Input
    override var noStdlibLink: Boolean = DokkaDefaults.noStdlibLink

    @Input
    override var noJdkLink: Boolean = DokkaDefaults.noJdkLink

    @Input
    var noAndroidSdkLink: Boolean = false

    @Input
    override var suppressedFiles: List<String> = emptyList()

    @Input
    override var analysisPlatform: Platform = DokkaDefaults.analysisPlatform

    @Input
    @Optional
    var platform: String? = null

    @Internal
    @Transient
    var collectKotlinTasks: (() -> List<Any?>?)? = null

    fun DokkaSourceSetID(sourceSetName: String): DokkaSourceSetID {
        return DokkaSourceSetID(project, sourceSetName)
    }

    fun dependsOn(sourceSet: GradleSourceSet) {
        dependsOn(DokkaSourceSetID(sourceSet.name))
    }

    fun dependsOn(sourceSet: DokkaSourceSet) {
        dependsOn(sourceSet.sourceSetID)
    }

    fun dependsOn(sourceSetName: String) {
        dependsOn(DokkaSourceSetID(sourceSetName))
    }

    fun dependsOn(sourceSetID: DokkaSourceSetID) {
        dependentSourceSets.add(sourceSetID)
    }

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
        externalDocumentationLinks.add(ExternalDocumentationLink.Builder(link.url, link.packageListUrl).build())
    }

    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkImpl>) {
        val link = GradleExternalDocumentationLinkImpl()
        action.execute(link)
        externalDocumentationLinks.add(ExternalDocumentationLink.Builder(link.url, link.packageListUrl).build())
    }
}

fun GradleDokkaSourceSet.dependsOn(sourceSet: KotlinSourceSet) {
    dependsOn(DokkaSourceSetID(sourceSet.name))
}

fun GradleDokkaSourceSet.dependsOn(sourceSet: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet) {
    dependsOn(DokkaSourceSetID(sourceSet.name))
}

fun GradleDokkaSourceSet.dependsOn(sourceSet: AndroidSourceSet) {
    dependsOn(DokkaSourceSetID(sourceSet.name))
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

class GradleDokkaModuleDescription : DokkaModuleDescription {
    override var name: String = ""
    override var path: String = ""
    override var docFile: String = ""
}

class GradleDokkaConfigurationImpl : DokkaConfiguration {
    override var outputDir: String = ""
    override var cacheRoot: String? = DokkaDefaults.cacheRoot
    override var offlineMode: Boolean = DokkaDefaults.offlineMode
    override var failOnWarning: Boolean = DokkaDefaults.failOnWarning
    override var sourceSets: List<GradleDokkaSourceSet> = emptyList()
    override var pluginsClasspath: List<File> = emptyList()
    override var pluginsConfiguration: Map<String, String> = mutableMapOf()
    override var modules: List<GradleDokkaModuleDescription> = emptyList()
}

class GradlePackageOptionsImpl : PackageOptions, Serializable {
    override var prefix: String = ""
    override var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic
    override var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented
    override var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated
    override var suppress: Boolean = DokkaDefaults.suppress
}

internal fun GradleDokkaSourceSet.copy(): GradleDokkaSourceSet {
    val newObj = GradleDokkaSourceSet(this.name, this.project)
    this::class.memberProperties.forEach { field ->
        if (field is KMutableProperty<*>) {
            when (val value = field.getter.call(this)) {
                is List<*> -> field.setter.call(newObj, value.toMutableList())
                is Set<*> -> field.setter.call(newObj, value.toMutableSet())
                else -> field.setter.call(newObj, field.getter.call(this))
            }

        }
    }
    return newObj
}
