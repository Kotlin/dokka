package org.jetbrains.dokka.gradle

import com.google.gson.GsonBuilder
import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.dokka.*
import org.jetbrains.dokka.ReflectDsl.isNotInstance
import org.jetbrains.dokka.gradle.ClassloaderContainer.fatJarClassLoader
import org.jetbrains.dokka.gradle.DokkaVersion.version

import java.io.File
import java.io.InputStream
import java.io.Serializable
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.Callable
import java.util.function.BiConsumer

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))

        val passConfiguration= project.container(GradlePassConfigurationImpl::class.java)
        project.extensions.add("passConfigurations", passConfiguration)

        project.tasks.create("dokka", DokkaTask::class.java).apply {
            dokkaRuntime = project.configurations.create("dokkaRuntime")
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
    @Input var reportUndocumented = true
    @Input var perPackageOptions: MutableList<PackageOptions> = arrayListOf()
    @Input var impliedPlatforms: MutableList<String> = arrayListOf()

    @Input var externalDocumentationLinks = mutableListOf<DokkaConfiguration.ExternalDocumentationLink>()

    @Input var noStdlibLink: Boolean = false

    @Input
    var noJdkLink: Boolean = false

    @Optional @Input
    var cacheRoot: String? = null


    @Optional @Input
    var languageVersion: String? = null

    @Optional @Input
    var apiVersion: String? = null

    @Input
    var collectInheritedExtensionsFromLibraries: Boolean = false

    @get:Internal
    internal val kotlinCompileBasedClasspathAndSourceRoots: ClasspathAndSourceRoots by lazy { extractClasspathAndSourceRootsFromKotlinTasks() }

    @Optional @Input
    var targets: List<String> = listOf()

//    @Input var passConfigurations: MutableList<GradlePassConfigurationImpl> = arrayListOf()

//    fun passConfiguration(action: Action<GradlePassConfigurationImpl>) {
//        val passConfig = GradlePassConfigurationImpl()
//        action.execute(passConfig)
//        passConfigurations.add(passConfig)
//    }
//
//    fun passConfiguration(closure: Closure<Unit>) {
//        passConfiguration(Action {
//            closure.delegate = it
//            closure.call()
//        })
//    }

//    fun passConfiguration(closure: Closure<Unit>) {
//        closure.call()
//        passConfiguration = mutableListOf(PassConfigurationImpl(targets = closure.getProperty("targets") as List<String>))
//    }

    private var kotlinTasksConfigurator: () -> List<Any?>? = { defaultKotlinTasks() }
    private val kotlinTasks: List<Task> by lazy { extractKotlinCompileTasks() }

    fun kotlinTasks(taskSupplier: Callable<List<Any>>) {
        kotlinTasksConfigurator = { taskSupplier.call() }
    }

    fun kotlinTasks(closure: Closure<Any?>) {
        kotlinTasksConfigurator = { closure.call() as? List<Any?> }
    }

    fun linkMapping(action: Action<LinkMapping>) {
        val mapping = LinkMapping()
        action.execute(mapping)

        if (mapping.path.isEmpty()) {
            throw IllegalArgumentException("Link mapping should have dir")
        }
        if (mapping.url.isEmpty()) {
            throw IllegalArgumentException("Link mapping should have url")
        }

        linkMappings.add(mapping)
    }

    fun linkMapping(closure: Closure<Unit>) {
        linkMapping(Action { mapping ->
            closure.delegate = mapping
            closure.call()
        })
    }

    fun sourceRoot(action: Action<SourceRoot>) {
        val sourceRoot = SourceRoot()
        action.execute(sourceRoot)
        sourceRoots.add(sourceRoot)
    }

    fun sourceRoot(closure: Closure<Unit>) {
        sourceRoot(Action { sourceRoot ->
            closure.delegate = sourceRoot
            closure.call()
        })
    }

    fun packageOptions(action: Action<PackageOptions>) {
        val packageOptions = PackageOptions()
        action.execute(packageOptions)
        perPackageOptions.add(packageOptions)
    }

    fun packageOptions(closure: Closure<Unit>) {
        packageOptions(Action { packageOptions ->
            closure.delegate = packageOptions
            closure.call()
        })
    }

    fun externalDocumentationLink(action: Action<DokkaConfiguration.ExternalDocumentationLink.Builder>) {
        val builder = DokkaConfiguration.ExternalDocumentationLink.Builder()
        action.execute(builder)
        externalDocumentationLinks.add(builder.build())
    }

    fun externalDocumentationLink(closure: Closure<Unit>) {
        externalDocumentationLink(Action { builder ->
            closure.delegate = builder
            closure.call()
        })
    }

    fun tryResolveFatJar(project: Project): Set<File> {
        return try {
            dokkaRuntime!!.resolve()
        } catch (e: Exception) {
            project.parent?.let { tryResolveFatJar(it) } ?: throw e
        }
    }

    fun loadFatJar() {
        if (fatJarClassLoader == null) {
            val jars = if (dokkaFatJar is File)
                setOf(dokkaFatJar as File)
            else
                tryResolveFatJar(project)
            fatJarClassLoader = URLClassLoader(jars.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader().parent)
        }
    }

    internal data class ClasspathAndSourceRoots(val classpathFileCollection: FileCollection, val sourceRoots: List<File>) : Serializable

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

    private fun Iterable<File>.toSourceRoots(): List<SourceRoot> = this.filter { it.exists() }.map { SourceRoot().apply { path = it.path } }

    protected open fun collectSuppressedFiles(sourceRoots: List<SourceRoot>): List<String> = emptyList()

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

            val (tasksClasspath, tasksSourceRoots) = kotlinCompileBasedClasspathAndSourceRoots

            val sourceRoots = collectSourceRoots() + tasksSourceRoots.toSourceRoots()
            if (sourceRoots.isEmpty()) {
                logger.warn("No source directories found: skipping dokka generation")
                return
            }

            val fullClasspath = tasksClasspath + classpath

            val bootstrapClass = fatJarClassLoader!!.loadClass("org.jetbrains.dokka.DokkaBootstrapImpl")
            val bootstrapInstance = bootstrapClass.constructors.first().newInstance()
            val bootstrapProxy: DokkaBootstrap = automagicTypedProxy(javaClass.classLoader, bootstrapInstance)

            val gson = GsonBuilder().setPrettyPrinting().create()


            val configuration = GradleDokkaConfigurationImpl()
            configuration.outputDir = outputDirectory
            configuration.format = outputFormat
            configuration.generateIndexPages = true
            configuration.cacheRoot = cacheRoot
            configuration.impliedPlatforms = impliedPlatforms
            configuration.passesConfigurations =
                (project.extensions.getByName("passConfigurations") as Iterable<GradlePassConfigurationImpl>)
                .toList()
                .map { defaultPassConfiguration(it) }

//                listOf(PassConfigurationImpl(
//                    classpath= fullClasspath.map { it.absolutePath },
//                    sourceRoots = sourceRoots.map { SourceRootImpl(it.path) },
//                    samples = samples.filterNotNull().map { project.file(it).absolutePath },
//                    includes = includes.filterNotNull().map { project.file(it).absolutePath },
//                    collectInheritedExtensionsFromLibraries = collectInheritedExtensionsFromLibraries,
//                    perPackageOptions = perPackageOptions.map{PackageOptionsImpl(
//                        prefix = it.prefix,
//                        includeNonPublic = it.includeNonPublic,
//                        reportUndocumented = it.reportUndocumented,
//                        skipDeprecated = it.skipDeprecated,
//                        suppress = it.suppress
//                        )},
//                    moduleName = moduleName,
//                    includeNonPublic = includeNonPublic,
//                    includeRootPackage = false,
//                    reportUndocumented = reportUndocumented,
//                    skipEmptyPackages = skipEmptyPackages,
//                    skipDeprecated = skipDeprecated,
//                    jdkVersion = jdkVersion,
//                    languageVersion = languageVersion,
//                    apiVersion = apiVersion,
//                    noStdlibLink = noStdlibLink,
//                    noJdkLink = noJdkLink,
//                    suppressedFiles = collectSuppressedFiles(sourceRoots),
//                    sinceKotlin = "1.0",
//                    analysisPlatform = Platform.DEFAULT,
//                    targets = emptyList(),
//                    sourceLinks = mutableListOf(), // TODO: check this line
//                    externalDocumentationLinks = externalDocumentationLinks.map {
//                        ExternalDocumentationLinkImpl(it.url, it.packageListUrl)
//                        }
//                    )
//                )
//            )

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

        fun Closure<Unit>.setDelegateAndCall(delegate: Any) {
            this.delegate = delegate
            this.call()
            this.delegate
        }

        val (tasksClasspath, tasksSourceRoots) = kotlinCompileBasedClasspathAndSourceRoots

        val fullClasspath = tasksClasspath + classpath
        passConfig.moduleName = moduleName
        passConfig.classpath = fullClasspath.map { it.absolutePath }
        passConfig.sourceRoots = sourceRoots.map { SourceRootImpl(it.path) }
        passConfig.samples = samples.filterNotNull().map { project.file(it).absolutePath }
        passConfig.includes = includes.filterNotNull().map { project.file(it).absolutePath }
        passConfig.collectInheritedExtensionsFromLibraries = collectInheritedExtensionsFromLibraries

        passConfig.perPackageOptions = ((passConfig.perPackageOptions as List<Closure<Unit>>).map {
            it.setDelegateAndCall(GradlePackageOptionsImpl())
        }) as List<PackageOptionsImpl>

        passConfig.suppressedFiles = collectSuppressedFiles(sourceRoots)
        passConfig.sourceLinks = ((passConfig.sourceLinks as List<Closure<Unit>>).map {
            it.setDelegateAndCall(GradleSourceLinkDefinitionImpl())
        }) as MutableList<GradleSourceLinkDefinitionImpl> // TODO: Parse source links?

        passConfig.externalDocumentationLinks = ((passConfig.externalDocumentationLinks as List<Closure<Unit>>).map {
            it.setDelegateAndCall(GradleExternalDocumentationLinkImpl())
        }) as List<ExternalDocumentationLinkImpl>

        return passConfig
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


    @Classpath
    fun getInputClasspath(): FileCollection {
        val (classpathFileCollection) = extractClasspathAndSourceRootsFromKotlinTasks()
        return project.files(classpath) + classpathFileCollection
    }

    @InputFiles
    fun getInputFiles(): FileCollection {
        val (_, tasksSourceRoots) = extractClasspathAndSourceRootsFromKotlinTasks()
        return project.files(tasksSourceRoots.map { project.fileTree(it) }) +
                project.files(collectSourceRoots().map { project.fileTree(File(it.path)) }) +
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

    override fun toString(): String = path
}

open class LinkMapping : Serializable, DokkaConfiguration.SourceLinkDefinition {
    var dir: String
        get() = path
        set(value) {
            if (value.contains("\\"))
                throw java.lang.IllegalArgumentException("Incorrect dir property, only Unix based path allowed.")
            else path = value
        }

    override var path: String = ""
    override var url: String = ""

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

open class GradlePassConfigurationImpl(@Transient val name: String = ""): DokkaConfiguration.PassConfiguration {
    override var classpath: List<String> = emptyList()
    override var moduleName: String = ""
    override var sourceRoots: List<SourceRootImpl> = emptyList()
    override var samples: List<String> = emptyList()
    override var includes: List<String> = emptyList()
    override var includeNonPublic: Boolean = false
    override var includeRootPackage: Boolean = false
    override var reportUndocumented: Boolean = false
    override var skipEmptyPackages: Boolean = false
    override var skipDeprecated: Boolean = false
    override var jdkVersion: Int = 6
    override var sourceLinks: List<GradleSourceLinkDefinitionImpl> = emptyList()
    override var perPackageOptions: List<PackageOptionsImpl> = emptyList()
    override var externalDocumentationLinks: List<ExternalDocumentationLinkImpl> = emptyList()
    override var languageVersion: String? = null
    override var apiVersion: String? = null
    override var noStdlibLink: Boolean = false
    override var noJdkLink: Boolean = false
    override var suppressedFiles: List<String> = emptyList()
    override var collectInheritedExtensionsFromLibraries: Boolean = false
    override var analysisPlatform: Platform = Platform.DEFAULT
    override var targets: List<String> = listOf("JVM")
    override var sinceKotlin: String = "1.0"
}

class GradleSourceLinkDefinitionImpl : DokkaConfiguration.SourceLinkDefinition {
    override var path: String = ""
    override var url: String = ""
    override var lineSuffix: String? = null

    companion object {
        fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinitionImpl {
            val (path, urlAndLine) = srcLink.split('=')
            return SourceLinkDefinitionImpl(
                File(path).canonicalPath,
                urlAndLine.substringBefore("#"),
                urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#$it" })
        }
    }
}

class GradleExternalDocumentationLinkImpl : DokkaConfiguration.ExternalDocumentationLink {
    override var url: URL = URL("")
    override var packageListUrl: URL = URL("")
}

class GradleDokkaConfigurationImpl: DokkaConfiguration {
    override var outputDir: String = ""
    override var format: String = "html"
    override var generateIndexPages: Boolean = false
    override var cacheRoot: String? = null
    override var impliedPlatforms: List<String> = emptyList()
    override var passesConfigurations: List<GradlePassConfigurationImpl> = emptyList()
}

class GradlePackageOptionsImpl: DokkaConfiguration.PackageOptions {
    override var prefix: String = ""
    override val includeNonPublic: Boolean = false
    override val reportUndocumented: Boolean = true
    override val skipDeprecated: Boolean = true
    override val suppress: Boolean = false
}