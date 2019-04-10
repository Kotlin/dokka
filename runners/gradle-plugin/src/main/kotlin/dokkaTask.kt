package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ReflectDsl
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.automagicTypedProxy
import java.io.File
import java.io.Serializable
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
    @OutputDirectory
    var outputDirectory: String = ""
    var dokkaRuntime: Configuration? = null

    @InputFiles
    var classpath: Iterable<File> = arrayListOf()

    @Input
    var sourceDirs: Iterable<File> = emptyList()
    @Input
    var sourceRoots: MutableList<DokkaConfiguration.SourceRoot> = arrayListOf()
    @Input
    var dokkaFatJar: Any = "org.jetbrains.dokka:dokka-fatjar:${DokkaVersion.version}"
    @Input
    var impliedPlatforms: MutableList<String> = arrayListOf()
    @Optional
    @Input
    var cacheRoot: String? = null
    @Input
    var collectInheritedExtensionsFromLibraries: Boolean = false
    @get:Internal
    internal val kotlinCompileBasedClasspathAndSourceRoots: ClasspathAndSourceRoots by lazy { extractClasspathAndSourceRootsFromKotlinTasks() }

    protected var externalDocumentationLinks: MutableList<DokkaConfiguration.ExternalDocumentationLink> = mutableListOf()

    private var kotlinTasksConfigurator: () -> List<Any?>? = { defaultKotlinTasks() }
    private val kotlinTasks: List<Task> by lazy { extractKotlinCompileTasks() }

    fun kotlinTasks(taskSupplier: Callable<List<Any>>) {
        kotlinTasksConfigurator = { taskSupplier.call() }
    }

    fun kotlinTasks(closure: Closure<Any?>) {
        kotlinTasksConfigurator = { closure.call() as? List<Any?> }
    }


    fun tryResolveFatJar(project: Project): Set<File> {
        return try {
            dokkaRuntime!!.resolve()
        } catch (e: Exception) {
            project.parent?.let { tryResolveFatJar(it) } ?: throw e
        }
    }

    fun loadFatJar() {
        if (ClassloaderContainer.fatJarClassLoader == null) {
            val jars = if (dokkaFatJar is File)
                setOf(dokkaFatJar as File)
            else
                tryResolveFatJar(project)
            ClassloaderContainer.fatJarClassLoader = URLClassLoader(jars.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader().parent)
        }
    }

    internal data class ClasspathAndSourceRoots(val classpathFileCollection: FileCollection, val sourceRoots: List<File>) :
        Serializable

    private fun extractKotlinCompileTasks(): List<Task> {
        val inputList = (kotlinTasksConfigurator.invoke() ?: emptyList()).filterNotNull()
        val (paths, other) = inputList.partition { it is String }

        val taskContainer = project.tasks

        val tasksByPath = paths.map { taskContainer.findByPath(it as String) ?: throw IllegalArgumentException("Task with path '$it' not found") }

        other
            .filter { it !is Task || it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal entry in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE or String, but was $it") }

        tasksByPath
            .filter { it == null || it isNotInstance getAbstractKotlinCompileFor(it) }
            .forEach { throw IllegalArgumentException("Illegal task path in kotlinTasks, must be subtype of $ABSTRACT_KOTLIN_COMPILE, but was $it") }


        return (tasksByPath + other) as List<Task>
    }

    private fun extractClasspathAndSourceRootsFromKotlinTasks(): ClasspathAndSourceRoots {

        val allTasks = kotlinTasks

        val allClasspath = mutableSetOf<File>()
        var allClasspathFileCollection: FileCollection = project.files()
        val allSourceRoots = mutableSetOf<File>()

        allTasks.forEach {
            logger.debug("Dokka found AbstractKotlinCompile task: $it")
            with(ReflectDsl) {
                val taskSourceRoots: List<File> = it["sourceRootsContainer"]["sourceRoots"].v()

                val abstractKotlinCompileClz = getAbstractKotlinCompileFor(it)!!

                val taskClasspath: Iterable<File> =
                    (it["getClasspath", AbstractCompile::class].takeIfIsFunc()?.invoke()
                            ?: it["compileClasspath", abstractKotlinCompileClz].takeIfIsProp()?.v()
                            ?: it["getClasspath", abstractKotlinCompileClz]())

                if (taskClasspath is FileCollection) {
                    allClasspathFileCollection += taskClasspath
                } else {
                    allClasspath += taskClasspath
                }
                allSourceRoots += taskSourceRoots.filter { it.exists() }
            }
        }

        return ClasspathAndSourceRoots(allClasspathFileCollection + project.files(allClasspath), allSourceRoots.toList())
    }

    private fun Iterable<File>.toSourceRoots(): List<GradleSourceRootImpl> = this.filter { it.exists() }.map { GradleSourceRootImpl().apply { path = it.path } }

    protected open fun collectSuppressedFiles(sourceRoots: List<DokkaConfiguration.SourceRoot>): List<String> = emptyList()

    @TaskAction
    fun generate() {
        if (dokkaRuntime == null){
            dokkaRuntime = project.configurations.getByName("dokkaRuntime")
        }

        dokkaRuntime?.defaultDependencies{ dependencies -> dependencies.add(project.dependencies.create(dokkaFatJar)) }
        val kotlinColorsEnabledBefore = System.getProperty(COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(COLORS_ENABLED_PROPERTY, "false")
        try {
            loadFatJar()
            // TODO: implement extracting source roots from kotlin tasks
            val (_, tasksSourceRoots) = kotlinCompileBasedClasspathAndSourceRoots

            val sourceRoots = collectSourceRoots(sourceDirs, sourceRoots) + tasksSourceRoots.toSourceRoots()

            val bootstrapClass = ClassloaderContainer.fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")
            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
            val bootstrapProxy: DokkaBootstrap = automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

            val gson = GsonBuilder().setPrettyPrinting().create()

            val passConfigurationExtension: GradlePassConfigurationImpl? = this.extensions.findByName("passConfiguration") as GradlePassConfigurationImpl
            val passConfigurationsContainer by lazy {
                (this.extensions.getByName("passConfigurations") as Iterable<GradlePassConfigurationImpl>).toList()
            }
            passConfigurationExtension?.sourceRoots?.addAll(sourceRoots)

            val passConfigurationList =
                (passConfigurationExtension?.let {passConfigurationsContainer + it } ?: passConfigurationsContainer)
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

    private fun defaultPassConfiguration(passConfig: GradlePassConfigurationImpl): GradlePassConfigurationImpl{
        val (tasksClasspath, tasksSourceRoots) = kotlinCompileBasedClasspathAndSourceRoots

        val fullClasspath = tasksClasspath + classpath
        passConfig.moduleName = moduleName
        passConfig.classpath = fullClasspath.map { it.absolutePath }
        passConfig.samples = passConfig.samples.map { project.file(it).absolutePath }
        passConfig.includes = passConfig.includes.map { project.file(it).absolutePath }
        passConfig.collectInheritedExtensionsFromLibraries = collectInheritedExtensionsFromLibraries
        passConfig.suppressedFiles = collectSuppressedFiles(passConfig.sourceRoots)
        passConfig.externalDocumentationLinks.addAll(externalDocumentationLinks)

        return passConfig
    }

    private fun collectSourceRoots(sourceDirs: Iterable<File>, sourceRoots: List<DokkaConfiguration.SourceRoot>): List<DokkaConfiguration.SourceRoot> {
        val sourceDirs = when {
            sourceDirs.any() -> {
                logger.info("Dokka: Taking source directories provided by the user")
                sourceDirs.toSet()
            }
            kotlinTasks.isEmpty() -> project.convention.findPlugin(JavaPluginConvention::class.java)?.let { javaPluginConvention ->
                logger.info("Dokka: Taking source directories from default java plugin")
                val sourceSets = javaPluginConvention.sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
                sourceSets?.allSource?.srcDirs
            }
            else -> emptySet()
        }

        return sourceRoots + (sourceDirs?.toSourceRoots() ?: emptyList())
    }

//    @Classpath
//    fun getInputClasspath(): FileCollection {
//        val (classpathFileCollection) = extractClasspathAndSourceRootsFromKotlinTasks()
//        return project.files(classpath) + classpathFileCollection
//    }

//    @InputFiles
//    fun getInputFiles(): FileCollection {
//        val (_, tasksSourceRoots) = extractClasspathAndSourceRootsFromKotlinTasks()
//        return project.files(tasksSourceRoots.map { project.fileTree(it) }) +
//                project.files(collectSourceRoots().map { project.fileTree(File(it.path)) }) +
//                project.files(includes) +
//                project.files(samples.filterNotNull().map { project.fileTree(it) })
//    }

//    @OutputDirectory
//    fun getOutputDirectoryAsFile(): File = project.file(outputDirectory)

    companion object {
        const val COLORS_ENABLED_PROPERTY = "kotlin.colors.enabled"
        const val ABSTRACT_KOTLIN_COMPILE = "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"

        private fun getAbstractKotlinCompileFor(task: Task) = try {
            task.project.buildscript.classLoader.loadClass(ABSTRACT_KOTLIN_COMPILE)
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}
