package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Builder
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.gradle.ConfigurationExtractor.PlatformData
import org.jetbrains.dokka.plugability.Configurable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Callable
import java.util.function.BiConsumer
import kotlin.system.exitProcess

open class DokkaTask : DefaultTask(), Configurable {
    private val ANDROID_REFERENCE_URL = Builder("https://developer.android.com/reference/").build()
    private val GLOBAL_CONFIGURATION_NAME = "global" // Used for copying perPackageOptions to other platforms
    private val configExtractor = ConfigurationExtractor(project)

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
    override val pluginsConfiguration: Map<String, String> = mutableMapOf()

    @Optional
    @Input
    var cacheRoot: String? = null

    @Classpath
    lateinit var pluginsClasspathConfiguration: Configuration

    var dokkaSourceSets: NamedDomainObjectContainer<GradlePassConfigurationImpl>
        @Suppress("UNCHECKED_CAST")
        @Nested get() = (DslObject(this).extensions.getByName(SOURCE_SETS_EXTENSION_NAME) as NamedDomainObjectContainer<GradlePassConfigurationImpl>)
        internal set(value) = DslObject(this).extensions.add(SOURCE_SETS_EXTENSION_NAME, value)

    private val kotlinTasks: List<Task> by lazy {
        extractKotlinCompileTasks({
            dokkaSourceSets.map {
                it.collectKotlinTasks?.invoke()
            }
        }.takeIf { it().isNotEmpty() } ?: { defaultKotlinTasks() }
        )
    }

    @Input
    var disableAutoconfiguration: Boolean = false

    @Input
    var offlineMode: Boolean = false

    private var outputDiagnosticInfo: Boolean =
        false // Workaround for Gradle, which fires some methods (like collectConfigurations()) multiple times in its lifecycle

    private fun loadCore() {
        if (ClassloaderContainer.coreClassLoader == null) {
            val jars = dokkaRuntime!!.resolve()
            ClassloaderContainer.coreClassLoader = URLClassLoader(
                jars.map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getSystemClassLoader().parent
            )
        }
    }

    protected fun extractKotlinCompileTasks(collectTasks: () -> List<Any?>?): List<Task> {
        val inputList = (collectTasks.invoke() ?: emptyList()).filterNotNull()
        val (paths, other) = inputList.partition { it is String }

        val tasksByPath = paths.map {
            project.tasks.findByPath(it as String) ?: throw IllegalArgumentException("Task with path '$it' not found")
        }

        other
            .filter { it !is Task || it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal entry in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE or String, but was $it") }

        tasksByPath
            .filter { it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal task path in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE, but was $it") }

        @Suppress("UNCHECKED_CAST")
        return (tasksByPath + other) as List<Task>
    }

    private fun Iterable<File>.toSourceRoots(): List<GradleSourceRootImpl> =
        this.filter { it.exists() }.map { GradleSourceRootImpl().apply { path = it.path } }

    private fun Iterable<String>.toProjects(): List<Project> =
        project.subprojects.toList().filter { this.contains(it.name) }

    protected open fun collectSuppressedFiles(sourceRoots: List<SourceRoot>) =
        if (project.isAndroidProject()) {
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
    fun generate() = getConfiguration()?.let { generate(it) } ?: exitProcess(0)

    protected open fun generate(configuration: GradleDokkaConfigurationImpl) {
        outputDiagnosticInfo = true
        val kotlinColorsEnabledBefore = System.getProperty(COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(COLORS_ENABLED_PROPERTY, "false")
        try {
            loadCore()

            val bootstrapClass =
                ClassloaderContainer.coreClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")
            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
            val bootstrapProxy: DokkaBootstrap =
                automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

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
                GsonBuilder().setPrettyPrinting().create().toJson(configuration)
            )

            bootstrapProxy.generate()

        } finally {
            System.setProperty(COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
    }

    internal open fun getConfiguration(): GradleDokkaConfigurationImpl? {
        val globalConfig = dokkaSourceSets.toList().find { it.name.toLowerCase() == GLOBAL_CONFIGURATION_NAME }
        val defaultModulesConfiguration = passConfigurations
            .map { defaultPassConfiguration(it, globalConfig) }.takeIf { it.isNotEmpty() }
            ?: listOf(
                defaultPassConfiguration(
                    collectSinglePassConfiguration(GradlePassConfigurationImpl("main")),
                    null
                )
            ).takeIf { project.isNotMultiplatformProject() } ?: emptyList()

        if (defaultModulesConfiguration.isEmpty()) {
            logger.error("No source sets to document found, exiting")
            return null
        }

        return GradleDokkaConfigurationImpl().apply {
            outputDir = project.file(outputDirectory).absolutePath
            format = outputFormat
            cacheRoot = this@DokkaTask.cacheRoot
            offlineMode = this@DokkaTask.offlineMode
            passesConfigurations = defaultModulesConfiguration
            pluginsClasspath = pluginsClasspathConfiguration.resolve().toList()
            pluginsConfiguration = this@DokkaTask.pluginsConfiguration
        }
    }


    protected val passConfigurations: List<GradlePassConfigurationImpl>
        get() = dokkaSourceSets
            .filterNot { it.name.toLowerCase() == GLOBAL_CONFIGURATION_NAME }
            .map { collectSinglePassConfiguration(it) }

    protected fun collectSinglePassConfiguration(config: GradlePassConfigurationImpl): GradlePassConfigurationImpl {
        val userConfig = config
            .apply {
                collectKotlinTasks?.let {
                    configExtractor.extractFromKotlinTasks(extractKotlinCompileTasks(it))
                        .fold(this) { config, platformData ->
                            mergeUserConfigurationAndPlatformData(config, platformData)
                        }
                }
            }

        if (disableAutoconfiguration) return userConfig

        val baseConfig = configExtractor.extractConfiguration(userConfig.name)
            ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: if (this.dokkaSourceSets.isNotEmpty()) {
                if (outputDiagnosticInfo)
                    logger.warn(
                        "Could not find source set with name: ${userConfig.name} in Kotlin Gradle Plugin, " +
                                "using only user provided configuration for this source set"
                    )
                userConfig
            } else {
                if (outputDiagnosticInfo)
                    logger.warn("Could not find source set with name: ${userConfig.name} in Kotlin Gradle Plugin")
                collectFromSinglePlatformOldPlugin(userConfig.name, userConfig)
            }

        return if (subProjects.isNotEmpty()) {
            try {
                subProjects.toProjects().fold(baseConfig) { configAcc, subProject ->
                    mergeUserConfigurationAndPlatformData(
                        configAcc,
                        ConfigurationExtractor(subProject).extractConfiguration(userConfig.name)!!
                    )
                }
            } catch (e: NullPointerException) {
                logger.warn(
                    "Cannot extract sources from subProjects. Do you have the Kotlin plugin applied in the root project?"
                )
                baseConfig
            }
        } else {
            baseConfig
        }
    }

    protected fun collectFromSinglePlatformOldPlugin(name: String, userConfig: GradlePassConfigurationImpl) =
        kotlinTasks.find { it.name == name }
            ?.let { configExtractor.extractFromKotlinTasks(listOf(it)) }
            ?.singleOrNull()
            ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: configExtractor.extractFromJavaPlugin()
                ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: userConfig

    protected fun mergeUserConfigurationAndPlatformData(
        userConfig: GradlePassConfigurationImpl,
        autoConfig: PlatformData
    ) =
        userConfig.copy().apply {
            sourceSetID = autoConfig.name ?: ""
            sourceRoots.addAll(userConfig.sourceRoots.union(autoConfig.sourceRoots.toSourceRoots()).distinct())
            dependentSourceSets.addAll(userConfig.dependentSourceSets.union(autoConfig.dependentSourceSets).distinct())
            classpath = userConfig.classpath.union(autoConfig.classpath.map { it.absolutePath }).distinct()
            if (userConfig.platform == null && autoConfig.platform != "")
                platform = autoConfig.platform
        }

    protected fun defaultPassConfiguration(
        config: GradlePassConfigurationImpl,
        globalConfig: GradlePassConfigurationImpl?
    ): GradlePassConfigurationImpl {
        if (config.moduleName.isBlank()) {
            config.moduleName = project.name
        }
        if (config.sourceSetID.isBlank()) {
            config.sourceSetID = config.name.takeIf(String::isNotBlank) ?: config.analysisPlatform.key
        }
        config.displayName = config.sourceSetID.substringBeforeLast("Main")
        config.classpath =
            (config.classpath as List<Any>).map { it.toString() }.distinct() // Workaround for Groovy's GStringImpl
        config.sourceRoots = config.sourceRoots.distinct().toMutableList()
        config.samples = config.samples.map { project.file(it).absolutePath }
        config.includes = config.includes.map { project.file(it).absolutePath }
        config.suppressedFiles += collectSuppressedFiles(config.sourceRoots)
        if (project.isAndroidProject() && !config.noAndroidSdkLink) {
            config.externalDocumentationLinks.add(ANDROID_REFERENCE_URL)
        }
        if (config.platform?.isNotBlank() == true) {
            config.analysisPlatform = dokkaPlatformFromString(config.platform.toString())
        }
        globalConfig?.let {
            config.perPackageOptions.addAll(globalConfig.perPackageOptions)
            config.externalDocumentationLinks.addAll(globalConfig.externalDocumentationLinks)
            config.sourceLinks.addAll(globalConfig.sourceLinks)
            config.samples += globalConfig.samples.map { project.file(it).absolutePath }
            config.includes += globalConfig.includes.map { project.file(it).absolutePath }
        }
        return config
    }

    private fun dokkaPlatformFromString(platform: String) = when (platform.toLowerCase()) {
        "androidjvm", "android" -> Platform.jvm
        "metadata" -> Platform.common
        else -> Platform.fromString(platform)
    }

    // Needed for Gradle incremental build
    @OutputDirectory
    fun getOutputDirectoryAsFile(): File = project.file(outputDirectory)

    // Needed for Gradle incremental build
    @InputFiles
    fun getInputFiles(): FileCollection {
        val config = passConfigurations
        return project.files(config.flatMap { it.sourceRoots }.map { project.fileTree(File(it.path)) }) +
                project.files(config.flatMap { it.includes }) +
                project.files(config.flatMap { it.samples }.map { project.fileTree(File(it)) })
    }

    @Classpath
    fun getInputClasspath(): FileCollection =
        project.files((passConfigurations.flatMap { it.classpath } as List<Any>).map { project.fileTree(File(it.toString())) })

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

fun Project.dokka(configuration: DokkaTask.() -> Unit) {
    tasks.withType(DokkaTask::class.java) { dokkaTask ->
        dokkaTask.configuration()
    }
}
