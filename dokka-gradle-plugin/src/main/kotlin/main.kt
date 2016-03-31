package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DocumentationOptions
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.SourceLinkDefinition
import java.io.File
import java.util.*

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("dokka", DokkaTask::class.java).apply {
            moduleName = project.name
            outputDirectory = File(project.buildDir, "dokka").absolutePath
        }
    }
}

open class DokkaTask : DefaultTask() {
    init {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Generates dokka documentation for Kotlin"
    }

    @Input
    var moduleName: String = ""
    @Input
    var outputFormat: String = "html"
    var outputDirectory: String = ""
    @Input
    var processConfigurations: List<Any?> = arrayListOf("compile")
    @Input
    var includes: List<Any?> = arrayListOf()
    @Input
    var linkMappings: ArrayList<LinkMapping> = arrayListOf()
    @Input
    var samples: List<Any?> = arrayListOf()
    @Input
    var jdkVersion: Int = 6
    @Input
    var sourceDirs: Iterable<File> = emptyList()

    protected open val sdkProvider: SdkProvider? = null

    fun linkMapping(closure: Closure<Any?>) {
        val mapping = LinkMapping()
        closure.delegate = mapping
        closure.call()

        if (mapping.dir.isEmpty()) {
            throw IllegalArgumentException("Link mapping should have dir")
        }
        if (mapping.url.isEmpty()) {
            throw IllegalArgumentException("Link mapping should have url")
        }

        linkMappings.add(mapping)
    }

    @TaskAction
    fun generate() {
        val project = project
        val sdkProvider = sdkProvider
        val sourceDirectories = getSourceDirectories()
        val allConfigurations = project.configurations

        val classpath =
                if (sdkProvider != null && sdkProvider.isValid) sdkProvider.classpath else emptyList<File>() +
                        processConfigurations
                                .map { allConfigurations?.getByName(it.toString()) ?: throw IllegalArgumentException("No configuration $it found") }
                                .flatMap { it }

        if (sourceDirectories.isEmpty()) {
            logger.warn("No source directories found: skipping dokka generation")
            return
        }

        DokkaGenerator(
                DokkaGradleLogger(logger),
                classpath.map { it.absolutePath },
                sourceDirectories.map { it.absolutePath },
                samples.filterNotNull().map { project.file(it).absolutePath },
                includes.filterNotNull().map { project.file(it).absolutePath },
                moduleName,
                DocumentationOptions(outputDirectory, outputFormat,
                        sourceLinks = linkMappings.map { SourceLinkDefinition(project.file(it.dir).absolutePath, it.url, it.suffix) },
                        jdkVersion = jdkVersion)
        ).generate()
    }

    fun getSourceDirectories(): Collection<File> {
        val provider = sdkProvider
        val sourceDirs = if (sourceDirs.any()) {
            logger.info("Dokka: Taking source directories provided by the user")
            sourceDirs.toSet()
        } else if (provider != null && provider.isValid) {
            logger.info("Dokka: Taking source directories from ${provider.name} sdk provider")
            provider.sourceDirs
        } else {
            logger.info("Dokka: Taking source directories from default java plugin")
            val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
            val sourceSets = javaPluginConvention.sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
            sourceSets?.allSource?.srcDirs
        }

        return sourceDirs?.filter { it.exists() } ?: emptyList()
    }

    @Input
    @InputFiles
    @SkipWhenEmpty
    fun getInputFiles(): FileCollection = project.files(getSourceDirectories().map { project.fileTree(it) }) +
            project.files(includes) +
            project.files(samples)

    @OutputDirectory
    fun getOutputDirectoryAsFile(): File = project.file(outputDirectory)

}

open class LinkMapping {
    var dir: String = ""
    var url: String = ""
    var suffix: String? = null
}

/**
 * A provider for SDKs that can be used if a project uses classes that live outside the JDK or uses a
 * different method to determine the source directories.
 *
 * For example an Android library project configures its sources through the Android extension instead
 * of the basic java convention. Also it has its custom classes located in the SDK installation directory.
 */
interface SdkProvider {
    /**
     * The name of this provider. Only used for logging purposes.
     */
    val name: String

    /**
     * Checks whether this provider has everything it needs to provide the source directories.
     */
    val isValid: Boolean

    /**
     * Provides additional classpath files where Dokka should search for external classes.
     * The file list is injected **after** JDK Jars and **before** project dependencies.
     *
     * This is only called if [isValid] returns `true`.
     */
    val classpath: List<File>

    /**
     * Provides a list of directories where Dokka should search for source files.
     *
     * This is only called if [isValid] returns `true`.
     */
    val sourceDirs: Set<File>?
}
