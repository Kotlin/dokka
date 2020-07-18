@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import com.android.build.gradle.api.AndroidSourceSet
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.gradle.util.ConfigureUtil
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.net.URL
import org.jetbrains.kotlin.gradle.model.SourceSet as KotlinModelSourceSet


internal fun Task.GradleDokkaSourceSetBuilderFactory(): (name: String) -> GradleDokkaSourceSetBuilder =
    { name -> GradleDokkaSourceSetBuilder(name, project) }

open class GradleDokkaSourceSetBuilder constructor(
    @get:JsonIgnore @Transient @get:Input val name: String,
    @get:JsonIgnore @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<DokkaSourceSetImpl> {

    @Classpath
    @Optional
    var classpath: List<File> = emptyList()

    @Input
    @Optional
    var moduleDisplayName: String? = null

    @Input
    @Optional
    var displayName: String? = null

    @get:Internal
    val sourceSetID: DokkaSourceSetID = DokkaSourceSetID(project, name)

    @Nested
    var sourceRoots: MutableList<GradleSourceRootBuilder> = mutableListOf()

    @Input
    var dependentSourceSets: MutableSet<DokkaSourceSetID> = mutableSetOf()

    @InputFiles
    @Optional
    var samples: List<File> = emptyList()

    @InputFiles
    @Optional
    var includes: List<File> = emptyList()

    @Input
    var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic

    @Input
    var includeRootPackage: Boolean = DokkaDefaults.includeRootPackage

    @Input
    var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

    @Input
    var skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages

    @Input
    var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

    @Input
    var jdkVersion: Int = DokkaDefaults.jdkVersion

    @Nested
    var sourceLinks: MutableList<GradleSourceLinkBuilder> = mutableListOf()

    @Nested
    var perPackageOptions: MutableList<GradlePackageOptionsBuilder> = mutableListOf()

    @Nested
    var externalDocumentationLinks: MutableList<GradleExternalDocumentationLinkBuilder> = mutableListOf()

    @Input
    @Optional
    var languageVersion: String? = null

    @Input
    @Optional
    var apiVersion: String? = null

    @Input
    var noStdlibLink: Boolean = DokkaDefaults.noStdlibLink

    @Input
    var noJdkLink: Boolean = DokkaDefaults.noJdkLink

    @Input
    var noAndroidSdkLink: Boolean = false

    @Input
    var suppressedFiles: List<File> = emptyList()

    @Input
    @Optional
    var analysisPlatform: Platform? = null

    @Input
    @Optional
    var platform: String? = null

    fun DokkaSourceSetID(sourceSetName: String): DokkaSourceSetID {
        return DokkaSourceSetID(project, sourceSetName)
    }

    fun dependsOn(sourceSet: SourceSet) {
        dependsOn(DokkaSourceSetID(sourceSet.name))
    }

    fun dependsOn(sourceSet: GradleDokkaSourceSetBuilder) {
        dependsOn(sourceSet.sourceSetID)
    }

    fun dependsOn(sourceSet: DokkaConfiguration.DokkaSourceSet) {
        dependsOn(sourceSet.sourceSetID)
    }

    fun dependsOn(sourceSetName: String) {
        dependsOn(DokkaSourceSetID(sourceSetName))
    }

    fun dependsOn(sourceSetID: DokkaSourceSetID) {
        dependentSourceSets.add(sourceSetID)
    }

    // TODO NOW: Cover with tests
    fun sourceRoot(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradleSourceRootBuilder())
        sourceRoots.add(configured)
    }

    fun sourceRoot(action: Action<in GradleSourceRootBuilder>) {
        val sourceRoot = GradleSourceRootBuilder()
        action.execute(sourceRoot)
        sourceRoots.add(sourceRoot)
    }

    fun sourceLink(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradleSourceLinkBuilder())
        sourceLinks.add(configured)
    }

    fun sourceLink(action: Action<in GradleSourceLinkBuilder>) {
        val sourceLink = GradleSourceLinkBuilder()
        action.execute(sourceLink)
        sourceLinks.add(sourceLink)
    }

    fun perPackageOption(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradlePackageOptionsBuilder())
        perPackageOptions.add(configured)
    }

    fun perPackageOption(action: Action<in GradlePackageOptionsBuilder>) {
        val option = GradlePackageOptionsBuilder()
        action.execute(option)
        perPackageOptions.add(option)
    }

    fun externalDocumentationLink(c: Closure<Unit>) {
        val link = ConfigureUtil.configure(c, GradleExternalDocumentationLinkBuilder())
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkBuilder>) {
        val link = GradleExternalDocumentationLinkBuilder()
        action.execute(link)
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(url: String, packageListUrl: String? = null) {
        externalDocumentationLinks.add(
            GradleExternalDocumentationLinkBuilder().apply {
                this.url = URL(url)
                this.packageListUrl = URL(packageListUrl)
            }
        )
    }

    fun externalDocumentationLink(url: URL, packageListUrl: URL? = null) {
        externalDocumentationLinks.add(
            GradleExternalDocumentationLinkBuilder().apply {
                this.url = url
                if (packageListUrl != null) {
                    this.packageListUrl = packageListUrl
                }
            }
        )
    }

    override fun build(): DokkaSourceSetImpl {
        val moduleDisplayName = moduleDisplayName ?: project.name

        val displayName = displayName ?: name.substringBeforeLast("Main", platform.toString())

        val externalDocumentationLinks = externalDocumentationLinks.map { it.build() }
            .run {
                if (noJdkLink) this
                else this + ExternalDocumentationLink(
                    url =
                    if (jdkVersion < 11) "https://docs.oracle.com/javase/${jdkVersion}/docs/api/"
                    else "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/java.base/",
                    packageListUrl =
                    if (jdkVersion < 11) "https://docs.oracle.com/javase/${jdkVersion}/docs/api/package-list"
                    else "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/element-list"
                )
            }
            .run {
                if (noStdlibLink) this
                else this + ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
            }
            .run {
                if (noAndroidSdkLink || !project.isAndroidProject()) this
                else this +
                        ExternalDocumentationLink("https://developer.android.com/reference/") +
                        ExternalDocumentationLink(
                            url = URL("https://developer.android.com/reference/kotlin/"),
                            packageListUrl = URL("https://developer.android.com/reference/androidx/package-list")
                        )
            }

        val analysisPlatform = when {
            analysisPlatform != null -> checkNotNull(analysisPlatform)

            platform?.isNotBlank() == true -> when (val platform = platform.toString().toLowerCase()) {
                "androidjvm", "android" -> Platform.jvm
                "metadata" -> Platform.common
                else -> Platform.fromString(platform)
            }

            else -> Platform.DEFAULT
        }

        val sourceRoots = sourceRoots.build().distinct()

        val suppressedFiles = suppressedFiles + project.collectSuppressedFiles(sourceRoots)

        return DokkaSourceSetImpl(
            classpath = classpath.distinct().toList(),
            moduleDisplayName = moduleDisplayName,
            displayName = displayName,
            sourceSetID = sourceSetID,
            sourceRoots = sourceRoots,
            dependentSourceSets = dependentSourceSets.toSet(),
            samples = samples.toList(),
            includes = includes.toList(),
            includeNonPublic = includeNonPublic,
            includeRootPackage = includeRootPackage,
            reportUndocumented = reportUndocumented,
            skipEmptyPackages = skipEmptyPackages,
            skipDeprecated = skipDeprecated,
            jdkVersion = jdkVersion,
            sourceLinks = sourceLinks.build(),
            perPackageOptions = perPackageOptions.build(),
            externalDocumentationLinks = externalDocumentationLinks,
            languageVersion = languageVersion,
            apiVersion = apiVersion,
            noStdlibLink = noStdlibLink,
            noJdkLink = noJdkLink,
            suppressedFiles = suppressedFiles,
            analysisPlatform = analysisPlatform
        )
    }
}

fun GradleDokkaSourceSetBuilder.dependsOn(sourceSet: KotlinModelSourceSet) {
    dependsOn(DokkaSourceSetID(sourceSet.name))
}

fun GradleDokkaSourceSetBuilder.dependsOn(sourceSet: KotlinSourceSet) {
    dependsOn(DokkaSourceSetID(sourceSet.name))
}

fun GradleDokkaSourceSetBuilder.dependsOn(sourceSet: AndroidSourceSet) {
    dependsOn(DokkaSourceSetID(sourceSet.name))
}

// TODO NOW: Test
private fun Project.collectSuppressedFiles(sourceRoots: List<DokkaConfiguration.SourceRoot>): List<File> =
    if (project.isAndroidProject()) {
        val generatedRoot = project.buildDir.resolve("generated").absoluteFile
        sourceRoots
            .map { it.directory }
            .filter { it.startsWith(generatedRoot) }
            .flatMap { it.walk().toList() }
    } else {
        emptyList()
    }
