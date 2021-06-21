@file:Suppress("FunctionName", "UnstableApiUsage")

package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.setProperty
import org.gradle.util.ConfigureUtil
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

open class GradleDokkaSourceSetBuilder(
    @Transient @get:Input val name: String,
    @Transient @get:Internal internal val project: Project,
    @Transient @get:Internal internal val sourceSetIdFactory: NamedDomainObjectFactory<DokkaSourceSetID>,
) : DokkaConfigurationBuilder<DokkaSourceSetImpl> {

    @Input
    val sourceSetID: DokkaSourceSetID = sourceSetIdFactory.create(name)

    @Input
    val suppress: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(false)

    @Classpath
    @Optional
    val classpath: ConfigurableFileCollection = project.files()

    @Input
    @Optional
    val displayName: Property<String?> = project.objects.safeProperty()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val sourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    val dependentSourceSets: SetProperty<DokkaSourceSetID> = project.objects.setProperty<DokkaSourceSetID>()
        .convention(emptySet())

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    val samples: ConfigurableFileCollection = project.files()

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
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
    val suppressGeneratedFiles: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(true)

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
    val languageVersion: Property<String?> = project.objects.safeProperty()

    @Input
    @Optional
    val apiVersion: Property<String?> = project.objects.safeProperty()

    @Input
    val noStdlibLink: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.noStdlibLink)

    @Input
    val noJdkLink: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.noJdkLink)

    @Input
    val noAndroidSdkLink: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(false)

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val suppressedFiles: ConfigurableFileCollection = project.files()

    @Input
    @Optional
    val platform: Property<Platform> = project.objects.safeProperty<Platform>()
        .safeConvention(Platform.DEFAULT)

    fun DokkaSourceSetID(sourceSetName: String): DokkaSourceSetID = sourceSetIdFactory.create(sourceSetName)

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

    fun sourceRoot(file: File) {
        sourceRoots.from(file)
    }

    fun sourceRoot(path: String) {
        sourceRoot(project.file(path))
    }

    fun sourceLink(c: Closure<in GradleSourceLinkBuilder>) {
        val configured = ConfigureUtil.configure(c, GradleSourceLinkBuilder(project))
        sourceLinks.add(configured)
    }

    fun sourceLink(action: Action<in GradleSourceLinkBuilder>) {
        val sourceLink = GradleSourceLinkBuilder(project)
        action.execute(sourceLink)
        sourceLinks.add(sourceLink)
    }

    fun perPackageOption(c: Closure<in GradlePackageOptionsBuilder>) {
        val configured = ConfigureUtil.configure(c, GradlePackageOptionsBuilder(project))
        perPackageOptions.add(configured)
    }

    fun perPackageOption(action: Action<in GradlePackageOptionsBuilder>) {
        val option = GradlePackageOptionsBuilder(project)
        action.execute(option)
        perPackageOptions.add(option)
    }

    fun externalDocumentationLink(c: Closure<in GradleExternalDocumentationLinkBuilder>) {
        val link = ConfigureUtil.configure(c, GradleExternalDocumentationLinkBuilder(project))
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkBuilder>) {
        val link = GradleExternalDocumentationLinkBuilder(project)
        action.execute(link)
        externalDocumentationLinks.add(link)
    }

    fun externalDocumentationLink(url: String, packageListUrl: String? = null) {
        externalDocumentationLink(URL(url), packageListUrl = packageListUrl?.let(::URL))
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

    override fun build(): DokkaSourceSetImpl = toDokkaSourceSetImpl()
}

