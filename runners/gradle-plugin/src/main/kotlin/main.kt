package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.automagicTypedProxy
import org.jetbrains.dokka.gradle.ClassloaderContainer.fatJarClassLoader
import org.jetbrains.dokka.gradle.DokkaVersion.version
import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.net.URLClassLoader
import java.util.*
import java.util.function.BiConsumer

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))
        project.tasks.create("dokka", DokkaTask::class.java).apply {
            moduleName = project.name
            outputDirectory = File(project.buildDir, "dokka").absolutePath
        }
    }
}

object DokkaVersion {
    var version: String? = null

    fun loadFrom(stream: InputStream) {
        version = Properties().apply {
            load(stream)
        }.getProperty("dokka-version")
    }
}


object ClassloaderContainer {
    @JvmField
    var fatJarClassLoader: ClassLoader? = null
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
    @Input
    var dokkaFatJar: Any = "org.jetbrains.dokka:dokka-fatjar:$version"

    @Input var skipDeprecated = false
    @Input var skipEmptyPackages = true
    @Input var reportNotDocumented = true


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


    fun tryResolveFatJar(project: Project): File {
        return try {
            val dependency = project.buildscript.dependencies.create(dokkaFatJar)
            val configuration = project.buildscript.configurations.detachedConfiguration(dependency)
            configuration.description = "Dokka main jar"
            configuration.resolve().first()
        } catch (e: Exception) {
            project.parent?.let { tryResolveFatJar(it) } ?: throw e
        }
    }

    fun loadFatJar() {
        if (fatJarClassLoader == null) {
            val fatjar = if (dokkaFatJar is File)
                dokkaFatJar as File
            else
                tryResolveFatJar(project)
            fatJarClassLoader = URLClassLoader(arrayOf(fatjar.toURI().toURL()), ClassLoader.getSystemClassLoader().parent)
        }
    }

    val COLORS_ENABLED_PROPERTY = "kotlin.colors.enabled"

    @TaskAction
    fun generate() {
        val kotlinColorsEnabledBefore = System.getProperty(COLORS_ENABLED_PROPERTY)
        System.setProperty(COLORS_ENABLED_PROPERTY, "false")
        try {
            loadFatJar()

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

            val bootstrapClass = fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")

            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()

            val bootstrapProxy: DokkaBootstrap = automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

            bootstrapProxy.configure(
                    BiConsumer { level, message ->
                        when (level) {
                            "info" -> logger.info(message)
                            "warn" -> logger.warn(message)
                            "error" -> logger.error(message)
                        }
                    },
                    moduleName,
                    classpath.map { it.absolutePath },
                    sourceDirectories.map { it.absolutePath },
                    samples.filterNotNull().map { project.file(it).absolutePath },
                    includes.filterNotNull().map { project.file(it).absolutePath },
                    outputDirectory,
                    outputFormat,
                    false,
                    false,
                    reportNotDocumented,
                    skipEmptyPackages,
                    skipDeprecated,
                    6,
                    true,
                    linkMappings.map {
                        val path = project.file(it.dir).absolutePath
                        "$path=${it.url}${it.suffix}"
                    })

            bootstrapProxy.generate()

        } finally {
            System.setProperty(COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
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

    @InputFiles
    @SkipWhenEmpty
    fun getInputFiles(): FileCollection =
            project.files(getSourceDirectories().map { project.fileTree(it) }) +
                    project.files(includes) +
                    project.files(samples.map { project.fileTree(it) })

    @OutputDirectory
    fun getOutputDirectoryAsFile(): File = project.file(outputDirectory)
}

open class LinkMapping : Serializable {
    var dir: String = ""
    var url: String = ""
    var suffix: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as LinkMapping

        if (dir != other.dir) return false
        if (url != other.url) return false
        if (suffix != other.suffix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dir.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (suffix?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val serialVersionUID: Long = -8133501684312445981L
    }
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
