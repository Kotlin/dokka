package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Builder
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.gradle.ConfigurationExtractor.PlatformData
import java.io.File
import java.net.URL
import java.util.concurrent.Callable

open class DokkaTask : AbstractDokkaTask() {
    private val ANDROID_REFERENCE_URL = Builder("https://developer.android.com/reference/").build()

    private val ANDROIDX_REFERENCE_URL = Builder(
        url = URL("https://developer.android.com/reference/kotlin/"),
        packageListUrl = URL("https://developer.android.com/reference/androidx/package-list")
    ).build()

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

    @Optional
    @Input
    var cacheRoot: String? = null


    /**
     * Hack used by DokkaCollector to enforce a different configuration to be used.
     */
    @get:Internal
    internal var enforcedConfiguration: GradleDokkaConfigurationImpl? = null

    @get:Nested
    val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSet> =
        project.container(GradleDokkaSourceSet::class.java) { name -> GradleDokkaSourceSet(name, project) }
            .also { container -> DslObject(this).extensions.add("dokkaSourceSets", container) }


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

    override fun generate() = enforcedConfiguration?.let { generate(it) } ?: generate(getConfigurationOrThrow())

    protected open fun generate(configuration: GradleDokkaConfigurationImpl) {
        outputDiagnosticInfo = true
        val bootstrap = DokkaBootstrap("org.jetbrains.dokka.DokkaBootstrapImpl")

        bootstrap.configure(
            GsonBuilder().setPrettyPrinting().create().toJson(configuration)
        ) { level, message ->
            when (level) {
                "debug" -> logger.debug(message)
                "info" -> logger.info(message)
                "progress" -> logger.lifecycle(message)
                "warn" -> logger.warn(message)
                "error" -> logger.error(message)
            }
        }
        bootstrap.generate()
    }


    @Internal
    internal fun getConfigurationOrNull(): GradleDokkaConfigurationImpl? {
        val defaultModulesConfiguration = configuredDokkaSourceSets
            .map { configureDefault(it) }.takeIf { it.isNotEmpty() }
            ?: listOf(
                configureDefault(configureDokkaSourceSet(dokkaSourceSets.create("main")))
            ).takeIf { project.isNotMultiplatformProject() } ?: emptyList()

        if (defaultModulesConfiguration.isEmpty()) {
            return null
        }

        return GradleDokkaConfigurationImpl().apply {
            outputDir = project.file(outputDirectory).absolutePath
            cacheRoot = this@DokkaTask.cacheRoot
            offlineMode = this@DokkaTask.offlineMode
            sourceSets = defaultModulesConfiguration
            pluginsClasspath = plugins.resolve().toList()
            pluginsConfiguration = this@DokkaTask.pluginsConfiguration
            failOnWarning = this@DokkaTask.failOnWarning
        }
    }

    @Internal
    internal fun getConfigurationOrThrow(): GradleDokkaConfigurationImpl {
        return getConfigurationOrNull() ?: throw DokkaException(
            """
                No source sets to document found. 
                Make source to configure at least one source set e.g.
                
                tasks { 
                    dokkaHtml {
                        dokkaSourceSets {
                            register("commonMain") {
                                displayName = "common"
                                platform = "common"
                            }
                        }
                    }
                }
                """
        )
    }

    @get:Internal
    protected val configuredDokkaSourceSets: List<GradleDokkaSourceSet>
        get() = dokkaSourceSets.map { configureDokkaSourceSet(it) }

    private fun configureDokkaSourceSet(config: GradleDokkaSourceSet): GradleDokkaSourceSet {
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

        return configExtractor.extractConfiguration(userConfig.name)
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
    }

    private fun collectFromSinglePlatformOldPlugin(name: String, userConfig: GradleDokkaSourceSet) =
        kotlinTasks.find { it.name == name }
            ?.let { configExtractor.extractFromKotlinTasks(listOf(it)) }
            ?.singleOrNull()
            ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: configExtractor.extractFromJavaPlugin()
                ?.let { mergeUserConfigurationAndPlatformData(userConfig, it) }
            ?: userConfig

    private fun mergeUserConfigurationAndPlatformData(
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

    private fun configureDefault(config: GradleDokkaSourceSet): GradleDokkaSourceSet {
        if (config.moduleDisplayName.isBlank()) {
            config.moduleDisplayName = project.name
        }

        if (config.displayName.isBlank()) {
            config.displayName = config.name.substringBeforeLast("Main", config.platform.toString())
        }

        if (project.isAndroidProject() && !config.noAndroidSdkLink) {
            config.externalDocumentationLinks.add(ANDROID_REFERENCE_URL)
            config.externalDocumentationLinks.add(ANDROIDX_REFERENCE_URL)
        }

        if (config.platform?.isNotBlank() == true) {
            config.analysisPlatform = dokkaPlatformFromString(config.platform.toString())
        }

        // Workaround for Groovy's GStringImpl
        config.classpath = (config.classpath as List<Any>).map { it.toString() }.distinct()
        config.sourceRoots = config.sourceRoots.distinct().toMutableList()
        config.samples = config.samples.map { project.file(it).absolutePath }
        config.includes = config.includes.map { project.file(it).absolutePath }
        config.suppressedFiles += collectSuppressedFiles(config.sourceRoots)

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
        project.files((configuredDokkaSourceSets.flatMap { it.classpath } as List<Any>)
            .map { project.fileTree(File(it.toString())) })

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
