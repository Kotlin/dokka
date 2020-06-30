package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Builder
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.gradle.ConfigurationExtractor.PlatformData
import org.jetbrains.dokka.plugability.Configurable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Callable
import java.util.function.BiConsumer

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

    internal var config: GradleDokkaConfigurationImpl? = null

    var dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSet>
        @Suppress("UNCHECKED_CAST")
        @Nested get() = (DslObject(this).extensions.getByName(SOURCE_SETS_EXTENSION_NAME) as NamedDomainObjectContainer<GradleDokkaSourceSet>)
        internal set(value) = DslObject(this).extensions.add(SOURCE_SETS_EXTENSION_NAME, value)

    private val kotlinTasks: List<Task> by lazy {
        extractKotlinCompileTasks(
            dokkaSourceSets.mapNotNull {
                it.collectKotlinTasks?.invoke()
            }.takeIf { it.isNotEmpty() }?.flatten() ?: defaultKotlinTasks()
        )
    }

    @Input
    var disableAutoconfiguration: Boolean = false

    @Input
    var failOnWarning: Boolean = false

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

    protected fun extractKotlinCompileTasks(collectTasks: List<Any?>?): List<Task> {
        val inputList = (collectTasks ?: emptyList()).filterNotNull()
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
    fun generate() = config?.let { generate(it) } ?: generate(getConfigurationOrThrow())

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

    internal fun getConfigurationOrNull(): GradleDokkaConfigurationImpl? {
        val globalConfig = dokkaSourceSets.toList().find { it.name.toLowerCase() == GLOBAL_CONFIGURATION_NAME }
        val defaultModulesConfiguration = configuredDokkaSourceSets
            .map { configureDefault(it, globalConfig) }.takeIf { it.isNotEmpty() }
            ?: listOf(
                configureDefault(configureDokkaSourceSet(GradleDokkaSourceSet("main", project)), null)
            ).takeIf { project.isNotMultiplatformProject() } ?: emptyList()

        if (defaultModulesConfiguration.isEmpty()) {
            return null
        }

        return GradleDokkaConfigurationImpl().apply {
            outputDir = project.file(outputDirectory).absolutePath
            format = outputFormat
            cacheRoot = this@DokkaTask.cacheRoot
            offlineMode = this@DokkaTask.offlineMode
            sourceSets = defaultModulesConfiguration
            pluginsClasspath = pluginsClasspathConfiguration.resolve().toList()
            pluginsConfiguration = this@DokkaTask.pluginsConfiguration
            failOnWarning = this@DokkaTask.failOnWarning
        }
    }

    internal fun getConfigurationOrThrow(): GradleDokkaConfigurationImpl {
        return getConfigurationOrNull() ?: throw DokkaException(
            """
                No source sets to document found. 
                Make source to configure at least one source set e.g.
                
                dokka {
                    dokkaSourceSets {
                        create("commonMain") {
                            displayName = "common"
                            platform = "common"
                        }
                    }
                }
                """
        )
    }


    protected val configuredDokkaSourceSets: List<GradleDokkaSourceSet>
        get() = dokkaSourceSets
            .filterNot { it.name.toLowerCase() == GLOBAL_CONFIGURATION_NAME }
            .map { configureDokkaSourceSet(it) }

    protected fun configureDokkaSourceSet(config: GradleDokkaSourceSet): GradleDokkaSourceSet {
        val userConfig = config
            .apply {
                collectKotlinTasks?.let {
                    configExtractor.extractFromKotlinTasks(extractKotlinCompileTasks(it()))
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

    protected fun collectFromSinglePlatformOldPlugin(name: String, userConfig: GradleDokkaSourceSet) =
        kotlinTasks.find { it.name == name }
            ?.let { configExtractor.extractFromKotlinTasks(listOf(it)) }
            ?.singleOrNull()
            ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: configExtractor.extractFromJavaPlugin()
                ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: userConfig

    protected fun mergeUserConfigurationAndPlatformData(
        userConfig: GradleDokkaSourceSet,
        autoConfig: PlatformData
    ) = userConfig.copy().apply {
        sourceRoots.addAll(userConfig.sourceRoots.union(autoConfig.sourceRoots.toSourceRoots()).distinct())
        dependentSourceSets.addAll(userConfig.dependentSourceSets)
        dependentSourceSets.addAll(autoConfig.dependentSourceSets.map { DokkaSourceSetID(project, it) })
        classpath = userConfig.classpath.union(autoConfig.classpath.map { it.absolutePath }).distinct()
        if (userConfig.platform == null && autoConfig.platform != "")
            platform = autoConfig.platform
    }

    protected fun configureDefault(
        config: GradleDokkaSourceSet,
        globalConfig: GradleDokkaSourceSet?
    ): GradleDokkaSourceSet {
        if (config.moduleDisplayName.isBlank()) {
            config.moduleDisplayName = project.name
        }

        if (config.displayName.isBlank()) {
            config.displayName = config.name.substringBeforeLast("Main", config.platform.toString())
        }
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
    fun getInputFiles(): FileCollection = configuredDokkaSourceSets.let { config ->
        project.files(config.flatMap { it.sourceRoots }.map { project.fileTree(File(it.path)) }) +
                project.files(config.flatMap { it.includes }) +
                project.files(config.flatMap { it.samples }.map { project.fileTree(File(it)) })
    }

    @Classpath
    fun getInputClasspath(): FileCollection =
        project.files((configuredDokkaSourceSets.flatMap { it.classpath } as List<Any>).map { project.fileTree(File(it.toString())) })

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
