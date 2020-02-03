package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Builder
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.gradle.ConfigurationExtractor.PlatformData
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Callable
import java.util.function.BiConsumer

open class DokkaTask : DefaultTask() {
    private val ANDROID_REFERENCE_URL = Builder("https://developer.android.com/reference/").build()
    private val GLOBAL_PLATFORM_NAME = "global" // Used for copying perPackageOptions to other platforms

    @Suppress("MemberVisibilityCanBePrivate")
    fun defaultKotlinTasks(): List<Task> = with(ReflectDsl) {
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
    var outputFormat: String = "html"

    @Input
    var outputDirectory: String = ""

    var dokkaRuntime: Configuration? = null

    @Input
    var subProjects: List<String> = emptyList()

    @Input
    var impliedPlatforms: MutableList<String> = arrayListOf()

    @Optional
    @Input
    var cacheRoot: String? = null

    @Classpath
    lateinit var pluginsConfiguration: Configuration

    var multiplatform: NamedDomainObjectContainer<GradlePassConfigurationImpl>
        @Suppress("UNCHECKED_CAST")
        @Nested get() = (DslObject(this).extensions.getByName(MULTIPLATFORM_EXTENSION_NAME) as NamedDomainObjectContainer<GradlePassConfigurationImpl>)
        internal set(value) = DslObject(this).extensions.add(MULTIPLATFORM_EXTENSION_NAME, value)

    var configuration: GradlePassConfigurationImpl
        @Suppress("UNCHECKED_CAST")
        @Nested get() = DslObject(this).extensions.getByType(GradlePassConfigurationImpl::class.java)
        internal set(value) = DslObject(this).extensions.add(CONFIGURATION_EXTENSION_NAME, value)

    // Configure Dokka with closure in Gradle Kotlin DSL
    fun configuration(action: Action<in GradlePassConfigurationImpl>) = action.execute(configuration)


    private val kotlinTasks: List<Task> by lazy { extractKotlinCompileTasks(configuration.collectKotlinTasks ?: { defaultKotlinTasks() }) }

    private val configExtractor = ConfigurationExtractor(project)

    @Input
    var disableAutoconfiguration: Boolean = false

    private var outputDiagnosticInfo: Boolean = false // Workaround for Gradle, which fires some methods (like collectConfigurations()) multiple times in its lifecycle

    private fun loadFatJar() {
        if (ClassloaderContainer.fatJarClassLoader == null) {
            val jars = dokkaRuntime!!.resolve()
            ClassloaderContainer.fatJarClassLoader = URLClassLoader(jars.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader().parent)
        }
    }

    private fun extractKotlinCompileTasks(collectTasks: () -> List<Any?>?): List<Task> {
        val inputList = (collectTasks.invoke() ?: emptyList()).filterNotNull()
        val (paths, other) = inputList.partition { it is String }

        val taskContainer = project.tasks

        val tasksByPath = paths.map { taskContainer.findByPath(it as String) ?: throw IllegalArgumentException("Task with path '$it' not found") }

        other
            .filter { it !is Task || it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal entry in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE or String, but was $it") }

        tasksByPath
            .filter { it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal task path in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE, but was $it") }

        @Suppress("UNCHECKED_CAST")
        return (tasksByPath + other) as List<Task>
    }

    private fun Iterable<File>.toSourceRoots(): List<GradleSourceRootImpl> = this.filter { it.exists() }.map { GradleSourceRootImpl().apply { path = it.path } }
    private fun Iterable<String>.toProjects(): List<Project> = project.subprojects.toList().filter { this.contains(it.name) }

    private fun collectSuppressedFiles(sourceRoots: List<SourceRoot>) =
        if(project.isAndroidProject()) {
            val generatedRoot = project.buildDir.resolve("generated").absoluteFile
            sourceRoots
                .map { File(it.path) }
                .filter { it.startsWith(generatedRoot) }
                .flatMap { it.walk().toList() }
                .map { it.absolutePath }
        } else {
            emptyList()
        }

    @TaskAction
    fun generate() {
        outputDiagnosticInfo = true
        val kotlinColorsEnabledBefore = System.getProperty(COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(COLORS_ENABLED_PROPERTY, "false")
        try {
            loadFatJar()

            val bootstrapClass = ClassloaderContainer.fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")
            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
            val bootstrapProxy: DokkaBootstrap =
                automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

            val gson = GsonBuilder().setPrettyPrinting().create()

            val globalConfig = multiplatform.toList().find { it.name.toLowerCase() == GLOBAL_PLATFORM_NAME }
            val passConfigurationList = collectConfigurations()
                .map { defaultPassConfiguration(it, globalConfig) }

            val configuration = GradleDokkaConfigurationImpl().apply {
                outputDir = outputDirectory
                format = outputFormat
                generateIndexPages = true
                cacheRoot = cacheRoot
                impliedPlatforms = impliedPlatforms
                passesConfigurations = passConfigurationList
                pluginsClasspath = pluginsConfiguration.resolve().toList()
            }

            bootstrapProxy.configure(
                BiConsumer { level, message ->
                    when (level) {
                        "debug" -> logger.debug(message)
                        "info" -> logger.info(message)
                        "progress" -> logger.lifecycle(message)
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

    private fun collectConfigurations() =
        if (this.isMultiplatformProject()) collectMultiplatform() else listOf(collectSinglePlatform(configuration))

    private fun collectMultiplatform() = multiplatform
        .filterNot { it.name.toLowerCase() == GLOBAL_PLATFORM_NAME }
        .map { collectSinglePlatform(it) }

    private fun collectSinglePlatform(config: GradlePassConfigurationImpl): GradlePassConfigurationImpl {
        val userConfig = config.let {
            if (it.collectKotlinTasks != null) {
                configExtractor.extractFromKotlinTasks(extractKotlinCompileTasks(it.collectKotlinTasks!!))
                    ?.let { platformData -> mergeUserConfigurationAndPlatformData(it, platformData) } ?: it
            } else {
                it
            }
        }

        if (disableAutoconfiguration) return userConfig

        val baseConfig = configExtractor.extractConfiguration(userConfig.name, userConfig.androidVariant)
            ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
                ?: if (this.isMultiplatformProject()) {
                    if (outputDiagnosticInfo)
                        logger.warn("Could not find target with name: ${userConfig.name} in Kotlin Gradle Plugin, " +
                                "using only user provided configuration for this target")
                    userConfig
                } else {
                    logger.warn("Could not find target with name: ${userConfig.name} in Kotlin Gradle Plugin")
                    collectFromSinglePlatformOldPlugin()
                }

        return if (subProjects.isNotEmpty()) {
            try {
                subProjects.toProjects().fold(baseConfig) { config, subProject ->
                    mergeUserConfigurationAndPlatformData(
                        config,
                        ConfigurationExtractor(subProject).extractConfiguration(config.name, config.androidVariant)!!
                    )
                }
            } catch(e: NullPointerException) {
                logger.warn("Cannot extract sources from subProjects. Do you have the Kotlin plugin in version 1.3.30+ " +
                        "and the Kotlin plugin applied in the root project?")
                baseConfig
            }
        } else {
            baseConfig
        }
    }

    private fun collectFromSinglePlatformOldPlugin() =
        configExtractor.extractFromKotlinTasks(kotlinTasks)
            ?.let { mergeUserConfigurationAndPlatformData(configuration, it) }
                ?: configExtractor.extractFromJavaPlugin()
                    ?.let { mergeUserConfigurationAndPlatformData(configuration, it) }
                ?: configuration

    private fun mergeUserConfigurationAndPlatformData(userConfig: GradlePassConfigurationImpl, autoConfig: PlatformData) =
        userConfig.copy().apply {
            sourceRoots.addAll(userConfig.sourceRoots.union(autoConfig.sourceRoots.toSourceRoots()).distinct())
            classpath = userConfig.classpath.union(autoConfig.classpath.map { it.absolutePath }).distinct()
            if (userConfig.platform == null && autoConfig.platform != "")
                platform = autoConfig.platform
        }

    private fun defaultPassConfiguration(
        config: GradlePassConfigurationImpl,
        globalConfig: GradlePassConfigurationImpl?
    ): GradlePassConfigurationImpl {
        if (config.moduleName == "") {
            config.moduleName = project.name
        }
        if (config.targets.isEmpty() && multiplatform.isNotEmpty()){
            config.targets = listOf(config.name)
        }
        config.classpath = (config.classpath as List<Any>).map { it.toString() }.distinct() // Workaround for Groovy's GStringImpl
        config.sourceRoots = config.sourceRoots.distinct().toMutableList()
        config.samples = config.samples.map { project.file(it).absolutePath }
        config.includes = config.includes.map { project.file(it).absolutePath }
        config.suppressedFiles += collectSuppressedFiles(config.sourceRoots)
        if (project.isAndroidProject() && !config.noAndroidSdkLink) { // TODO: introduce Android as a separate Dokka platform?
            config.externalDocumentationLinks.add(ANDROID_REFERENCE_URL)
        }
        if (config.platform != null && config.platform.toString().isNotEmpty()) {
            config.analysisPlatform = dokkaPlatformFromString(config.platform.toString())
        }
        if (globalConfig != null) {
            config.perPackageOptions.addAll(globalConfig.perPackageOptions)
            config.externalDocumentationLinks.addAll(globalConfig.externalDocumentationLinks)
            config.sourceLinks.addAll(globalConfig.sourceLinks)
            config.samples += globalConfig.samples.map { project.file(it).absolutePath }
            config.includes += globalConfig.includes.map { project.file(it).absolutePath }
        }
        return config
    }

    private fun dokkaPlatformFromString(platform: String) = when (platform.toLowerCase()) {
        KotlinPlatformType.androidJvm.toString().toLowerCase(), "androidjvm", "android" -> Platform.jvm
        "metadata" -> Platform.common
        else -> Platform.fromString(platform)
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
