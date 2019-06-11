package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Callable
import java.util.function.BiConsumer

open class DokkaTask : DefaultTask() {

    fun defaultKotlinTasks() = with(ReflectDsl) {
        val abstractKotlinCompileClz = try {
            project.buildscript.classLoader.loadClass(ABSTRACT_KOTLIN_COMPILE)
        } catch (cnfe: ClassNotFoundException) {
            logger.warn("$ABSTRACT_KOTLIN_COMPILE class not found, default kotlin tasks ignored")
            return@with emptyList<Task>()
        }

        return@with project.tasks.filter { it isInstance abstractKotlinCompileClz }.filter { "Test" !in it.name }
    }

    init {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Generates dokka documentation for Kotlin"

        @Suppress("LeakingThis")
        dependsOn(Callable { kotlinTasks.map { it.taskDependencies } })
    }

    @Input
    var moduleName: String = ""

    @Input
    var outputFormat: String = "html"

    var outputDirectory: String = ""

    var dokkaRuntime: Configuration? = null

    var defaultDokkaRuntime: Configuration? = null

    @Input
    var dokkaFatJar: String = "dokka-fatjar-${DokkaVersion.version}"

    private val defaultDokkaFatJar = "dokka-fatjar-${DokkaVersion.version}"

    @Input
    var impliedPlatforms: MutableList<String> = arrayListOf()

    @Optional
    @Input
    var cacheRoot: String? = null

    var multiplatform: NamedDomainObjectContainer<GradlePassConfigurationImpl>
        @Suppress("UNCHECKED_CAST")
        get() = DslObject(this).extensions.getByName(MULTIPLATFORM_EXTENSION_NAME) as NamedDomainObjectContainer<GradlePassConfigurationImpl>
        internal set(value) = DslObject(this).extensions.add(MULTIPLATFORM_EXTENSION_NAME, value)

    var configuration: GradlePassConfigurationImpl
        @Suppress("UNCHECKED_CAST")
        get() = DslObject(this).extensions.getByName(CONFIGURATION_EXTENSION_NAME) as GradlePassConfigurationImpl
        internal set(value) = DslObject(this).extensions.add(CONFIGURATION_EXTENSION_NAME, value)

    protected var externalDocumentationLinks: MutableList<DokkaConfiguration.ExternalDocumentationLink> = mutableListOf()

    private var kotlinTasksConfigurator: () -> List<Any?>? = { defaultKotlinTasks() }
    private val kotlinTasks: List<Task> by lazy { extractKotlinCompileTasks() }

    @Deprecated("Use manual configuration of source roots or subProjects{} closure")
    fun kotlinTasks(taskSupplier: Callable<List<Any>>) {
        kotlinTasksConfigurator = { taskSupplier.call() }
    }

    @Deprecated("Use manual configuration of source roots or subProjects{} closure")
    fun kotlinTasks(closure: Closure<Any?>) {
        kotlinTasksConfigurator = { closure.call() as? List<Any?> }
    }

    @Input
    var subProjects: List<String> = emptyList()

    fun tryResolveFatJar(configuration: Configuration?): Set<File> {
        return try {
            configuration!!.resolve()
        } catch (e: Exception) {
            project.parent?.let { tryResolveFatJar(configuration) } ?: throw e
        }
    }

    fun loadFatJar() {
        if (ClassloaderContainer.fatJarClassLoader == null) {
            val jars = tryResolveFatJar(dokkaRuntime).toList().union(tryResolveFatJar(defaultDokkaRuntime).toList()).filter { it.name.contains(dokkaFatJar) || it.name.contains(defaultDokkaFatJar) }
            ClassloaderContainer.fatJarClassLoader = URLClassLoader(jars.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader().parent)
        }
    }

    private fun extractKotlinCompileTasks(): List<Task> {
        val inputList = (kotlinTasksConfigurator.invoke() ?: emptyList()).filterNotNull()
        val (paths, other) = inputList.partition { it is String }

        val taskContainer = project.tasks

        val tasksByPath = paths.map { taskContainer.findByPath(it as String) ?: throw IllegalArgumentException("Task with path '$it' not found") }

        other
            .filter { it !is Task || it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal entry in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE or String, but was $it") }

        tasksByPath
            .filter { it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal task path in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE, but was $it") }


        return (tasksByPath + other) as List<Task>
    }

    private fun Iterable<File>.toSourceRoots(): List<GradleSourceRootImpl> = this.filter { it.exists() }.map { GradleSourceRootImpl().apply { path = it.path } }
    private fun Iterable<String>.toProjects(): List<Project> = project.subprojects.toList().filter { this.contains(it.name) }

    protected open fun collectSuppressedFiles(sourceRoots: List<SourceRoot>): List<String> = emptyList()

    @TaskAction
    fun generate() {
        val kotlinColorsEnabledBefore = System.getProperty(COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(COLORS_ENABLED_PROPERTY, "false")
        try {
            loadFatJar()

            val bootstrapClass = ClassloaderContainer.fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")
            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
            val bootstrapProxy: DokkaBootstrap =
                automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

            val gson = GsonBuilder().setPrettyPrinting().create()

            val passConfigurationList = collectConfigurations()
                .map { defaultPassConfiguration(it) }

            val configuration = GradleDokkaConfigurationImpl()
            configuration.outputDir = outputDirectory
            configuration.format = outputFormat
            configuration.generateIndexPages = true
            configuration.cacheRoot = cacheRoot
            configuration.impliedPlatforms = impliedPlatforms
            configuration.passesConfigurations = passConfigurationList

            bootstrapProxy.configure(
                BiConsumer { level, message ->
                    when (level) {
                        "info" -> logger.info(message)
                        "warn" -> logger.warn(message)
                        "error" -> logger.error(message)
                    }
                },
                gson.toJson(configuration)
            )

            bootstrapProxy.generate()

        } finally {
            System.setProperty(COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
    }

    private fun collectConfigurations(): List<GradlePassConfigurationImpl> =
        if (multiplatform.toList().isNotEmpty()) collectFromMultiPlatform() else collectFromSinglePlatform()

    private fun collectFromMultiPlatform(): List<GradlePassConfigurationImpl> {
        val baseConfig = mergeUserAndAutoConfigurations(
            multiplatform.toList(),
            ConfigurationExtractor.extractFromMultiPlatform(project).orEmpty()
        )
        return if (subProjects.isEmpty())
            baseConfig
        else
            subProjects.toProjects().fold(baseConfig, { list, project ->
                mergeUserAndAutoConfigurations(list, ConfigurationExtractor.extractFromMultiPlatform(project).orEmpty())})
    }

    private fun collectFromSinglePlatform(): List<GradlePassConfigurationImpl> {
        val autoConfig = ConfigurationExtractor.extractFromSinglePlatform(project)
        val baseConfig =  if (autoConfig != null)
            listOf(mergeUserConfigurationAndPlatformData(configuration, autoConfig))
        else
            collectFromSinglePlatformOldPlugin()

        return if (subProjects.isNotEmpty()) {
            try {
                subProjects.toProjects().fold(baseConfig, { list, project ->
                    listOf(mergeUserConfigurationAndPlatformData(list.first(), ConfigurationExtractor.extractFromSinglePlatform(project)!!))
                })
            } catch(e: NoClassDefFoundError) {
                logger.warn("Cannot extract sources from subProjects. Please update Kotlin plugin to version 1.3.30+")
                baseConfig
            }
        } else {
            baseConfig
        }
    }

    private fun collectFromSinglePlatformOldPlugin(): List<GradlePassConfigurationImpl> {
        val kotlinTasks = ConfigurationExtractor.extractFromKotlinTasks(kotlinTasks, project)
        return if (kotlinTasks != null) {
            listOf(mergeUserConfigurationAndPlatformData(configuration, kotlinTasks))
        } else {
            val javaPlugin = ConfigurationExtractor.extractFromJavaPlugin(project)
            if (javaPlugin != null)
                listOf(mergeUserConfigurationAndPlatformData(configuration, javaPlugin)) else listOf(configuration)
        }
    }

    private fun mergeUserAndAutoConfigurations(userConfigurations: List<GradlePassConfigurationImpl>,
                                               autoConfigurations: List<ConfigurationExtractor.PlatformData>): List<GradlePassConfigurationImpl> {
        val merged: MutableList<GradlePassConfigurationImpl> = mutableListOf()
        merged.addAll(
            userConfigurations.map { userConfig ->
                val autoConfig = autoConfigurations.find { autoConfig -> autoConfig.name == userConfig.name }
                if (autoConfig != null) mergeUserConfigurationAndPlatformData(userConfig, autoConfig) else userConfig
            }
        )
        return merged.toList()
    }

    private fun mergeUserConfigurationAndPlatformData(userConfig: GradlePassConfigurationImpl,
                                                      autoConfig: ConfigurationExtractor.PlatformData): GradlePassConfigurationImpl {
        val merged = userConfig.copy()
        merged.apply {
            sourceRoots.addAll(userConfig.sourceRoots.union(autoConfig.sourceRoots.toSourceRoots()).distinct())
            classpath = userConfig.classpath.union(autoConfig.classpath.map { it.absolutePath }).distinct()
            if (userConfig.platform == null)
                platform = autoConfig.platform
        }
        return merged
    }

    private fun defaultPassConfiguration(config: GradlePassConfigurationImpl): GradlePassConfigurationImpl {
        if (config.moduleName == "") {
            config.moduleName = moduleName
        }
        config.classpath = (config.classpath as List<Any>).map { it.toString() } // Workaround for Groovy's GStringImpl
        config.samples = config.samples.map { project.file(it).absolutePath }
        config.includes = config.includes.map { project.file(it).absolutePath }
        config.suppressedFiles += collectSuppressedFiles(config.sourceRoots)
        config.externalDocumentationLinks.addAll(externalDocumentationLinks)
        if (config.platform != null && config.platform.toString().isNotEmpty()){
            config.analysisPlatform = Platform.fromString(config.platform.toString())
        }
        return config
    }

    // Needed for Gradle incremental build
    @OutputDirectory
    fun getOutputDirectoryAsFile(): File = project.file(outputDirectory)

    // Needed for Gradle incremental build
    @InputFiles
    fun getInputFiles(): FileCollection {
        val config = collectConfigurations()
        return project.files(config.flatMap { it.sourceRoots }.map { project.fileTree(File(it.path)) }) +
                project.files(config.flatMap { it.includes }) +
                project.files(config.flatMap { it.samples }.map { project.fileTree(File(it)) })
    }

    @Classpath
    fun getInputClasspath(): FileCollection =
        project.files((collectConfigurations().flatMap { it.classpath } as List<Any>).map { project.fileTree(File(it.toString())) })

    companion object {
        const val COLORS_ENABLED_PROPERTY = "kotlin.colors.enabled"
        const val ABSTRACT_KOTLIN_COMPILE = "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"

        internal fun getAbstractKotlinCompileFor(task: Task) = try {
            task.project.buildscript.classLoader.loadClass(ABSTRACT_KOTLIN_COMPILE)
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}
