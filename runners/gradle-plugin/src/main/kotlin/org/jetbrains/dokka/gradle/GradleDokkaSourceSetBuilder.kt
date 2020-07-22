@file:Suppress("FunctionName", "UnstableApiUsage")

package org.jetbrains.dokka.gradle

import com.android.build.gradle.api.AndroidSourceSet
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.gradle.util.ConfigureUtil
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.net.URL
import org.jetbrains.kotlin.gradle.model.SourceSet as KotlinModelSourceSet

internal fun Task.GradleDokkaSourceSetBuilderFactory(): (name: String) -> GradleDokkaSourceSetBuilder =
    { name -> GradleDokkaSourceSetBuilder(name, project) }


// TODO NOW: Cover with tests
open class GradleDokkaSourceSetBuilder constructor(
    @Transient @get:Input val name: String,
    @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<DokkaSourceSetImpl> {

    @Internal
    val sourceSetID: DokkaSourceSetID = DokkaSourceSetID(project, name)

    @Classpath
    @Optional
    val classpath: ConfigurableFileCollection = project.files()

    @Input
    @Optional
    val moduleDisplayName: Property<String?> = project.objects.safeProperty()

    @Input
    @Optional
    val displayName: Property<String?> = project.objects.safeProperty()

    @Nested
    val sourceRoots: ConfigurableFileCollection = project.files()

    @Input
    val dependentSourceSets: SetProperty<DokkaSourceSetID> = project.objects.setProperty<DokkaSourceSetID>()
        .convention(emptySet())

    @InputFiles
    @Optional
    val samples: ConfigurableFileCollection = project.files()

    @InputFiles
    @Optional
    val includes: ConfigurableFileCollection = project.files()

    @Input
    val includeNonPublic: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.includeNonPublic)

    @Input
    val reportUndocumented: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.reportUndocumented)

    @Input
    val skipEmptyPackages: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.skipEmptyPackages)

    @Input
    val skipDeprecated: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.skipDeprecated)

    @Input
    val jdkVersion: Property<Int> = project.objects.safeProperty<Int>()
        .safeConvention(DokkaDefaults.jdkVersion)

    @Nested
    val sourceLinks: SetProperty<GradleSourceLinkBuilder> = project.objects.setProperty<GradleSourceLinkBuilder>()
        .convention(emptySet())

    @Nested
    val perPackageOptions: ListProperty<GradlePackageOptionsBuilder> =
        project.objects.listProperty<GradlePackageOptionsBuilder>()
            .convention(emptyList())

    @Nested
    val externalDocumentationLinks: SetProperty<GradleExternalDocumentationLinkBuilder> =
        project.objects.setProperty<GradleExternalDocumentationLinkBuilder>()
            .convention(emptySet())

    @Input
    @Optional
    val languageVersion: Property<String?> = project.objects.safeProperty<String>()

    @Input
    @Optional
    val apiVersion: Property<String?> = project.objects.safeProperty()

    @Input
    val noStdlibLink: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.noStdlibLink)

    @Input
    val noJdkLink: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.noJdkLink)

    @Input
    val noAndroidSdkLink: Property<Boolean> = project.objects.property<Boolean>()
        .convention(false)

    @InputFiles
    val suppressedFiles: ConfigurableFileCollection = project.files()

    @Input
    @Optional
    val analysisPlatform: Property<Platform?> = project.objects.safeProperty()

    @Input
    @Optional
    val platform: Property<String?> = project.objects.safeProperty()

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

    fun kotlinSourceSet(kotlinSourceSet: KotlinSourceSet) {
        configureWithKotlinSourceSet(kotlinSourceSet)
    }

    fun sourceRoot(file: File) {
        sourceRoots.from(file)
    }

    fun sourceRoot(path: String) {
        sourceRoot(project.file(path))
    }

    fun sourceLink(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradleSourceLinkBuilder(project))
        sourceLinks.add(configured)
    }

    fun sourceLink(action: Action<in GradleSourceLinkBuilder>) {
        val sourceLink = GradleSourceLinkBuilder(project)
        action.execute(sourceLink)
        sourceLinks.add(sourceLink)
    }

    fun perPackageOption(c: Closure<Unit>) {
        val configured = ConfigureUtil.configure(c, GradlePackageOptionsBuilder(project))
        perPackageOptions.add(configured)
    }

    fun perPackageOption(action: Action<in GradlePackageOptionsBuilder>) {
        val option = GradlePackageOptionsBuilder(project)
        action.execute(option)
        perPackageOptions.add(option)
    }

    fun externalDocumentationLink(c: Closure<Unit>) {
        val link = ConfigureUtil.configure(c, GradleExternalDocumentationLinkBuilder(project))
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkBuilder>) {
        val link = GradleExternalDocumentationLinkBuilder(project)
        action.execute(link)
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(url: String, packageListUrl: String? = null) {
        externalDocumentationLinks.add(
            GradleExternalDocumentationLinkBuilder(project).apply {
                this.url by URL(url)
                this.packageListUrl by URL(packageListUrl)
            }
        )
    }

    fun externalDocumentationLink(url: URL, packageListUrl: URL? = null) {
        externalDocumentationLinks.add(
            GradleExternalDocumentationLinkBuilder(project).apply {
                this.url by url
                if (packageListUrl != null) {
                    this.packageListUrl by packageListUrl
                }
            }
        )
    }

    override fun build(): DokkaSourceSetImpl {
        return toDokkaSourceSetImpl()
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
