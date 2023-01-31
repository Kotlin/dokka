package org.jetbrains.dokka.gradle.tasks

import kotlinx.serialization.encodeToString
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.jsonMapper
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
import javax.inject.Inject

/**
 * Produces the Dokka Configuration for this project.
 *
 * Configurations from other modules (which are potentially from other Gradle subprojects) will also be included.
 */
@CacheableTask
abstract class DokkaConfigurationTask @Inject constructor(
    objects: ObjectFactory,
) : DokkaTask() {

    @get:Input
    abstract val moduleName: Property<String>

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
    abstract val sourceSets: DomainObjectSet<DokkaConfigurationKxs.DokkaSourceSetKxs>

    @get:InputFiles
    @get:Classpath
    abstract val pluginsClasspath: ConfigurableFileCollection

    @get:Input
    abstract val pluginsConfiguration: DomainObjectSet<DokkaConfigurationKxs.PluginConfigurationKxs>

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

    @get:OutputFile
    abstract val dokkaConfigurationJson: RegularFileProperty

    init {
        description = "Assembles Dokka a configuration file, to be used when executing Dokka"
    }


    @TaskAction
    fun generateConfiguration() {
        val dokkaConfiguration = buildDokkaConfiguration()

        val encodedModuleDesc = jsonMapper.encodeToString(dokkaConfiguration)

        logger.info(encodedModuleDesc)

        dokkaConfigurationJson.get().asFile.writeText(encodedModuleDesc)
    }

    private fun buildDokkaConfiguration(): DokkaConfigurationKxs {

        val moduleName = moduleName.get()
        val moduleVersion = moduleVersion.orNull?.takeIf { it != "unspecified" }
        val outputDir = outputDir.asFile.get()
        val cacheRoot = cacheRoot.asFile.get()
        val offlineMode = offlineMode.get()
        val sourceSets = sourceSets.toList()
        val pluginsClasspath = pluginsClasspath.files.toList()
        val pluginsConfiguration = pluginsConfiguration.toList()
        val failOnWarning = failOnWarning.get()
        val delayTemplateSubstitution = delayTemplateSubstitution.get()
        val suppressObviousFunctions = suppressObviousFunctions.get()
        val includes = includes.files
        val suppressInheritedMembers = suppressInheritedMembers.get()
        val finalizeCoroutines = finalizeCoroutines.get()

        val dokkaModuleDescriptors = dokkaModuleDescriptors()

        // construct the base configuration for THIS project
        val baseDokkaConfiguration = DokkaConfigurationKxs(
            cacheRoot = cacheRoot,
            delayTemplateSubstitution = delayTemplateSubstitution,
            failOnWarning = failOnWarning,
            finalizeCoroutines = finalizeCoroutines,
            includes = includes,
            moduleName = moduleName,
            moduleVersion = moduleVersion,
            modulesKxs = dokkaModuleDescriptors,
            offlineMode = offlineMode,
            outputDir = outputDir,
            pluginsClasspath = pluginsClasspath,
            pluginsConfiguration = pluginsConfiguration,
            sourceSets = sourceSets,
            suppressInheritedMembers = suppressInheritedMembers,
            suppressObviousFunctions = suppressObviousFunctions,
        )

        // fetch any configurations from OTHER subprojects
        val subprojectConfigs = dokkaSubprojectConfigurations.files.map { file ->
            val fileContent = file.readText()
            jsonMapper.decodeFromString(
                DokkaConfigurationKxs.serializer(),
                fileContent,
            )
        }

        // now, combine them:
        return subprojectConfigs.fold(baseDokkaConfiguration) { acc, it: DokkaConfigurationKxs ->
            acc.copy(
                sourceSets = acc.sourceSets + it.sourceSets,
                // TODO remove plugin classpath aggregation, plugin classpath should be shared via Gradle Configuration
                //      so Gradle can correctly de-duplicate jars
                pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath,
            )
        }
    }

    private fun dokkaModuleDescriptors(): List<DokkaConfigurationKxs.DokkaModuleDescriptionKxs> {
        return dokkaModuleDescriptorFiles.files.map { file ->
            val fileContent = file.readText()
            jsonMapper.decodeFromString(
                DokkaConfigurationKxs.DokkaModuleDescriptionKxs.serializer(),
                fileContent,
            )
        }
    }
}
