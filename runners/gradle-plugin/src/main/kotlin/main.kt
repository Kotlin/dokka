package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.dokka.*
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.gradle.ClassloaderContainer.fatJarClassLoader
import org.jetbrains.dokka.gradle.DokkaVersion.version
import ru.yole.jkid.JsonExclude
import ru.yole.jkid.serialization.serialize
import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.Callable
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

const val `deprecationMessage reportNotDocumented` = "Will be removed in 0.9.17, see dokka#243"

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


    @Deprecated("Going to be removed in 0.9.16, use classpath + sourceDirs instead if kotlinTasks is not suitable for you")
    @Input var processConfigurations: List<Any?> = emptyList()

    @InputFiles var classpath: Iterable<File> = arrayListOf()

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
    var sourceRoots: MutableList<SourceRoot> = arrayListOf()

    @Input
    var dokkaFatJar: Any = "org.jetbrains.dokka:dokka-fatjar:$version"

    @Input var includeNonPublic = false
    @Input var skipDeprecated = false
    @Input var skipEmptyPackages = true

    @Deprecated(`deprecationMessage reportNotDocumented`, replaceWith = ReplaceWith("reportUndocumented"))
    var reportNotDocumented
        get() = reportUndocumented
        set(value) {
            logger.warn("Dokka: reportNotDocumented is deprecated and " + `deprecationMessage reportNotDocumented`.decapitalize())
            reportUndocumented = value
        }

    @Input var reportUndocumented = true
    @Input var perPackageOptions: MutableList<PackageOptions> = arrayListOf()
    @Input var impliedPlatforms: MutableList<String> = arrayListOf()

    @Input var externalDocumentationLinks = mutableListOf<DokkaConfiguration.ExternalDocumentationLink>()

    @Input var noStdlibLink: Boolean = false

    @Optional @Input
    var cacheRoot: String? = null


    @Optional @Input
    var languageVersion: String? = null

    @Optional @Input
    var apiVersion: String? = null

    @get:Input
    internal val kotlinCompileBasedClasspathAndSourceRoots: ClasspathAndSourceRoots by lazy { extractClasspathAndSourceRootsFromKotlinTasks() }


    private var kotlinTasksConfigurator: () -> List<Any?>? = { defaultKotlinTasks() }
    private val kotlinTasks: List<Task> by lazy { extractKotlinCompileTasks() }

    fun kotlinTasks(closure: Closure<Any?>) {
        kotlinTasksConfigurator = { closure.call() as? List<Any?> }
    }

    fun linkMapping(closure: Closure<Unit>) {
        val mapping = LinkMapping()
        closure.delegate = mapping
        closure.call()

        if (mapping.path.isEmpty()) {
            throw IllegalArgumentException("Link mapping should have dir")
        }
        if (mapping.url.isEmpty()) {
            throw IllegalArgumentException("Link mapping should have url")
        }

        linkMappings.add(mapping)
    }

    fun sourceRoot(closure: Closure<Unit>) {
        val sourceRoot = SourceRoot()
        closure.delegate = sourceRoot
        closure.call()
        sourceRoots.add(sourceRoot)
    }

    fun packageOptions(closure: Closure<Unit>) {
        val packageOptions = PackageOptions()
        closure.delegate = packageOptions
        closure.call()
        perPackageOptions.add(packageOptions)
    }

    fun externalDocumentationLink(closure: Closure<Unit>) {
        val builder = DokkaConfiguration.ExternalDocumentationLink.Builder()
        closure.delegate = builder
        closure.call()
        externalDocumentationLinks.add(builder.build())
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

    internal data class ClasspathAndSourceRoots(val classpath: List<File>, val sourceRoots: List<File>) : Serializable

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
        val allSourceRoots = mutableSetOf<File>()

        allTasks.forEach {

            logger.debug("Dokka found AbstractKotlinCompile task: $it")
            with(ReflectDsl) {
                val taskSourceRoots: List<File> = it["sourceRootsContainer"]["sourceRoots"].v()

                val abstractKotlinCompileClz = getAbstractKotlinCompileFor(it)!!

                val taskClasspath: Iterable<File> =
                        (it["compileClasspath", abstractKotlinCompileClz].takeIfIsProp()?.v() ?:
                                it["getClasspath", abstractKotlinCompileClz]())

                allClasspath += taskClasspath.filter { it.exists() }
                allSourceRoots += taskSourceRoots.filter { it.exists() }
            }
        }

        return ClasspathAndSourceRoots(allClasspath.toList(), allSourceRoots.toList())
    }

    private fun Iterable<File>.toSourceRoots(): List<SourceRoot> = this.filter { it.exists() }.map { SourceRoot().apply { path = it.path } }

    protected open fun collectSuppressedFiles(sourceRoots: List<SourceRoot>): List<String> = emptyList()

    @TaskAction
    fun generate() {
        val kotlinColorsEnabledBefore = System.getProperty(COLORS_ENABLED_PROPERTY) ?: "false"
        System.setProperty(COLORS_ENABLED_PROPERTY, "false")
        try {
            loadFatJar()

            val (tasksClasspath, tasksSourceRoots) = kotlinCompileBasedClasspathAndSourceRoots

            val project = project
            val sourceRoots = collectSourceRoots() + tasksSourceRoots.toSourceRoots()

            if (sourceRoots.isEmpty()) {
                logger.warn("No source directories found: skipping dokka generation")
                return
            }

            val fullClasspath = collectClasspathFromOldSources() + tasksClasspath + classpath

            val bootstrapClass = fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")

            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()

            val bootstrapProxy: DokkaBootstrap = automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

            val configuration = SerializeOnlyDokkaConfiguration(
                    moduleName,
                    fullClasspath.map { it.absolutePath },
                    sourceRoots,
                    samples.filterNotNull().map { project.file(it).absolutePath },
                    includes.filterNotNull().map { project.file(it).absolutePath },
                    outputDirectory,
                    outputFormat,
                    includeNonPublic,
                    false,
                    reportUndocumented,
                    skipEmptyPackages,
                    skipDeprecated,
                    jdkVersion,
                    true,
                    linkMappings,
                    impliedPlatforms,
                    perPackageOptions,
                    externalDocumentationLinks,
                    noStdlibLink,
                    cacheRoot,
                    collectSuppressedFiles(sourceRoots),
                    languageVersion,
                    apiVersion)


            bootstrapProxy.configure(
                    BiConsumer { level, message ->
                        when (level) {
                            "info" -> logger.info(message)
                            "warn" -> logger.warn(message)
                            "error" -> logger.error(message)
                        }
                    },
                    serialize(configuration)
            )

            bootstrapProxy.generate()

        } finally {
            System.setProperty(COLORS_ENABLED_PROPERTY, kotlinColorsEnabledBefore)
        }
    }

    private fun collectClasspathFromOldSources(): List<File> {

        val allConfigurations = project.configurations

        val fromConfigurations =
                processConfigurations.flatMap { allConfigurations.getByName(it.toString()) }

        return fromConfigurations
    }

    private fun collectSourceRoots(): List<SourceRoot> {
        val sourceDirs = if (sourceDirs.any()) {
            logger.info("Dokka: Taking source directories provided by the user")
            sourceDirs.toSet()
        } else if (kotlinTasks.isEmpty()) {
            project.convention.findPlugin(JavaPluginConvention::class.java)?.let { javaPluginConvention ->
                logger.info("Dokka: Taking source directories from default java plugin")
                val sourceSets = javaPluginConvention.sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
                sourceSets?.allSource?.srcDirs
            }
        } else {
            emptySet()
        }

        return sourceRoots + (sourceDirs?.toSourceRoots() ?: emptyList())
    }


    @InputFiles
    fun getInputFiles(): FileCollection {
        val (tasksClasspath, tasksSourceRoots) = extractClasspathAndSourceRootsFromKotlinTasks()

        val fullClasspath = collectClasspathFromOldSources() + tasksClasspath + classpath

        return project.files(tasksSourceRoots.map { project.fileTree(it) }) +
                project.files(collectSourceRoots().map { project.fileTree(File(it.path)) }) +
                project.files(fullClasspath.map { project.fileTree(it) }) +
                project.files(includes) +
                project.files(samples.filterNotNull().map { project.fileTree(it) })
    }

    @OutputDirectory
    fun getOutputDirectoryAsFile(): File = project.file(outputDirectory)

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

class SourceRoot : DokkaConfiguration.SourceRoot, Serializable {
    override var path: String = ""
        set(value) {
            field = File(value).absolutePath
        }

    override var platforms: List<String> = arrayListOf()

    override fun toString(): String {
        return "${platforms.joinToString()}::$path"
    }
}

open class LinkMapping : Serializable, DokkaConfiguration.SourceLinkDefinition {
    @JsonExclude
    var dir: String
        get() = path
        set(value) {
            path = value
        }

    override var path: String = ""
    override var url: String = ""

    @JsonExclude
    var suffix: String?
        get() = lineSuffix
        set(value) {
            lineSuffix = value
        }

    override var lineSuffix: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as LinkMapping

        if (path != other.path) return false
        if (url != other.url) return false
        if (lineSuffix != other.lineSuffix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (lineSuffix?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val serialVersionUID: Long = -8133501684312445981L
    }
}

class PackageOptions : Serializable, DokkaConfiguration.PackageOptions {
    override var prefix: String = ""
    override var includeNonPublic: Boolean = false
    override var reportUndocumented: Boolean = true
    override var skipDeprecated: Boolean = false
    override var suppress: Boolean = false
}
