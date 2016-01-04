package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
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
    var processConfigurations: ArrayList<String> = arrayListOf("compile")
    @Input
    var includes: ArrayList<String> = arrayListOf()
    @Input
    var linkMappings: ArrayList<LinkMapping> = arrayListOf()
    @Input
    var samples: ArrayList<String> = arrayListOf()

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
        val sourceDirectories = getSourceDirectories()
        val allConfigurations = project.configurations

        val classpath =
                processConfigurations
                .map { allConfigurations?.getByName(it) ?: throw IllegalArgumentException("No configuration $it found") }
                .flatMap { it }

        if (sourceDirectories.isEmpty()) {
            logger.warn("No source directories found: skipping dokka generation")
            return
        }

        DokkaGenerator(
                DokkaGradleLogger(logger),
                classpath.map { it.absolutePath },
                sourceDirectories.map { it.absolutePath },
                samples,
                includes,
                moduleName,
                outputDirectory,
                outputFormat,
                linkMappings.map { SourceLinkDefinition(project.file(it.dir).absolutePath, it.url, it.suffix) },
                false
        ).generate()
    }

    fun getSourceDirectories(): List<File> {
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSets = javaPluginConvention.sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
        return sourceSets?.allSource?.srcDirs?.filter { it.exists() } ?: emptyList()
    }

    @InputFiles
    @SkipWhenEmpty
    fun getIncludedFiles() : FileCollection = project.files(getSourceDirectories().map { project.fileTree(it) })

    @OutputDirectory
    fun getOutputDirectoryAsFile() : File = project.file(outputDirectory)

}

open class LinkMapping {
    var dir: String = ""
    var url: String = ""
    var suffix: String? = null
}
