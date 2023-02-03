package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfigurationBuilder
import javax.inject.Inject

abstract class DokkaConfigurationGradleBuilder @Inject constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
) : DokkaConfigurationBuilder<DokkaConfigurationKxs>, Named {

    @get:Input
    val moduleName: Provider<String> = providers.provider { name }

    @get:Input
    @get:Optional
    abstract val moduleVersion: Property<String>

    @get:Internal
    // marked as Internal because this task does not use the directory contents, only the location
    val outputDir: DirectoryProperty = objects.directoryProperty()

    /**
     * Because [outputDir] must be [Internal] (so Gradle doesn't check the directory contents),
     * [outputDirPath] is required so Gradle can determine if the task is up-to-date.
     */
    @get:Input
    protected val outputDirPath: Provider<String> = outputDir.map { it.asFile.invariantSeparatorsPath }

    @get:Internal
    // marked as Internal because this task does not use the directory contents, only the location
    val cacheRoot: DirectoryProperty = objects.directoryProperty()

    /**
     * Because [cacheRoot] must be [Internal] (so Gradle doesn't check the directory contents),
     * [cacheRootPath] is required so Gradle can determine if the task is up-to-date.
     */
    @get:Input
    protected val cacheRootPath: Provider<String> = cacheRoot.map { it.asFile.invariantSeparatorsPath }

    @get:Input
    abstract val offlineMode: Property<Boolean>

    @get:Input
    abstract val sourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder>

    @get:InputFiles
    @get:Classpath
    abstract val pluginsClasspath: ConfigurableFileCollection

    @get:Input
    abstract val pluginsConfiguration: NamedDomainObjectContainer<DokkaPluginConfigurationGradleBuilder>

    /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
    @get:InputFiles
//    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val dokkaSubprojectConfigurations: ConfigurableFileCollection

    /** Dokka Module Configuration from other subprojects. */
    @get:InputFiles
//    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val dokkaModuleDescriptorFiles: ConfigurableFileCollection

    @get:Input
    abstract val failOnWarning: Property<Boolean>

    @get:Input
    abstract val delayTemplateSubstitution: Property<Boolean>

    @get:Input
    abstract val suppressObviousFunctions: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val includes: ConfigurableFileCollection

    @get:Input
    abstract val suppressInheritedMembers: Property<Boolean>

    @get:Input
    abstract val finalizeCoroutines: Property<Boolean>

    override fun build() =
        DokkaConfigurationKxs(
            moduleName = moduleName.get(),
            moduleVersion = moduleVersion.get(),
            outputDir = outputDir.get().asFile,
            cacheRoot = cacheRoot.get().asFile,
            offlineMode = offlineMode.get(),
            failOnWarning = failOnWarning.get(),
            sourceSets = sourceSets.map(DokkaSourceSetGradleBuilder::build),
            pluginsClasspath = pluginsClasspath.files.toList(),
            pluginsConfiguration = pluginsConfiguration.map { it.build() },
            delayTemplateSubstitution = delayTemplateSubstitution.get(),
            suppressObviousFunctions = suppressObviousFunctions.get(),
            includes = includes.files,
            suppressInheritedMembers = suppressInheritedMembers.get(),
            finalizeCoroutines = finalizeCoroutines.get(),
            modulesKxs = emptyList(), // TODO convert Dokka Module files to object...
        )
}
