package org.jetbrains.dokka.maven

import org.apache.maven.archiver.MavenArchiveConfiguration
import org.apache.maven.archiver.MavenArchiver
import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.artifact.resolver.ArtifactResolutionResult
import org.apache.maven.artifact.resolver.ResolutionErrorHandler
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.*
import org.apache.maven.project.MavenProject
import org.apache.maven.project.MavenProjectHelper
import org.apache.maven.repository.RepositorySystem
import org.codehaus.plexus.archiver.Archiver
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.codehaus.plexus.archiver.util.DefaultFileSet
import org.codehaus.plexus.util.xml.Xpp3Dom
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.io.File
import java.net.URL

abstract class AbstractDokkaMojo(private val defaultDokkaPlugins: List<Dependency>) : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected var mavenProject: MavenProject? = null

    /**
     * The current build session instance. This is used for
     * dependency resolver API calls via repositorySystem.
     */
    @Parameter(defaultValue = "\${session}", required = true, readonly = true)
    protected var session: MavenSession? = null

    @Component
    private var repositorySystem: RepositorySystem? = null

    @Component
    private var resolutionErrorHandler: ResolutionErrorHandler? = null

    @Parameter(defaultValue = "JVM")
    var displayName: String = "JVM"

    @Parameter
    var sourceSetName: String = "JVM"

    /**
     * Source code roots to be analyzed and documented.
     * Accepts directories and individual `.kt` / `.java` files.
     *
     * Default is `{project.compileSourceRoots}`.
     */
    @Parameter(required = true, defaultValue = "\${project.compileSourceRoots}")
    var sourceDirectories: List<String> = emptyList()

    /**
     * List of directories or files that contain sample functions which are referenced via
     * [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier) KDoc tag.
     */
    @Parameter
    var samples: List<String> = emptyList()

    /**
     * List of Markdown files that contain
     * [module and package documentation](https://kotlinlang.org/docs/dokka-module-and-package-docs.html).
     *
     * Contents of specified files will be parsed and embedded into documentation as module and package descriptions.
     *
     * Example of such a file:
     *
     * ```markdown
     * # Module kotlin-demo
     *
     * The module shows the Dokka usage.
     *
     * # Package org.jetbrains.kotlin.demo
     *
     * Contains assorted useful stuff.
     *
     * ## Level 2 heading
     *
     * Text after this heading is also part of documentation for `org.jetbrains.kotlin.demo`
     *
     * # Package org.jetbrains.kotlin.demo2
     *
     * Useful stuff in another package.
     * ```
     */
    @Parameter
    var includes: List<String> = emptyList()

    /**
     * Classpath for analysis and interactive samples.
     *
     * Useful if some types that come from dependencies are not resolved/picked up automatically.
     * Property accepts both `.jar` and `.klib` files.
     *
     * Default is `{project.compileClasspathElements}`.
     */
    @Parameter(required = true, defaultValue = "\${project.compileClasspathElements}")
    var classpath: List<String> = emptyList()

    /**
     * Specifies the location of the project source code on the Web. If provided, Dokka generates
     * "source" links for each declaration. See [SourceLinkMapItem] for more details.
     */
    @Parameter
    var sourceLinks: List<SourceLinkMapItem> = emptyList()

    /**
     * Display name used to refer to the project/module. Used for ToC, navigation, logging, etc.
     *
     * Default is `{project.artifactId}`.
     */
    @Parameter(required = true, defaultValue = "\${project.artifactId}")
    var moduleName: String = ""

    /**
     * Whether to skip documentation generation.
     *
     * Default is `false`.
     */
    @Parameter(required = false, defaultValue = "false")
    var skip: Boolean = false

    /**
     * JDK version to use when generating external documentation links for Java types.
     *
     * For instance, if you use [java.util.UUID] from JDK in some public declaration signature,
     * and this property is set to `8`, Dokka will generate an external documentation link
     * to [JDK 8 Javadocs](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) for it.
     *
     * Default is JDK 8.
     */
    @Parameter(required = false, defaultValue = "${DokkaDefaults.jdkVersion}")
    var jdkVersion: Int = DokkaDefaults.jdkVersion

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be overridden on package level by setting [PackageOptions.skipDeprecated].
     *
     * Default is `false`.
     */
    @Parameter
    var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

    /**
     * Whether to skip packages that contain no visible declarations after
     * various filters have been applied.
     *
     * For instance, if [skipDeprecated] is set to `true` and your package contains only
     * deprecated declarations, it will be considered to be empty.
     *
     * Default is `true`.
     */
    @Parameter
    var skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
     * after they have been filtered by [documentedVisibilities].
     *
     * This setting works well with [failOnWarning].
     *
     * Can be overridden for a specific package by setting [PackageOptions.reportUndocumented].
     *
     * Default is `false`.
     */
    @Parameter
    var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

    /**
     * Allows to customize documentation generation options on a per-package basis.
     *
     * @see PackageOptions for details
     */
    @Parameter
    var perPackageOptions: List<PackageOptions> = emptyList()

    /**
     * Allows linking to Dokka/Javadoc documentation of the project's dependencies.
     *
     * @see ExternalDocumentationLinkBuilder for details
     */
    @Parameter
    var externalDocumentationLinks: List<ExternalDocumentationLinkBuilder> = emptyList()

    /**
     * Whether to generate external documentation links that lead to API reference
     * documentation for Kotlin's standard library when declarations from it are used.
     *
     * Default is `false`, meaning links will be generated.
     */
    @Parameter(defaultValue = "${DokkaDefaults.noStdlibLink}")
    var noStdlibLink: Boolean = DokkaDefaults.noStdlibLink

    /**
     * Whether to generate external documentation links to JDK's Javadocs
     * when declarations from it are used.
     *
     * The version of JDK Javadocs is determined by [jdkVersion] property.
     *
     * Default is `false`, meaning links will be generated.
     */
    @Parameter(defaultValue = "${DokkaDefaults.noJdkLink}")
    var noJdkLink: Boolean = DokkaDefaults.noJdkLink

    /**
     * Whether to resolve remote files/links over network.
     *
     * This includes package-lists used for generating external documentation links:
     * for instance, to make classes from standard library clickable.
     *
     * Setting this to `true` can significantly speed up build times in certain cases,
     * but can also worsen documentation quality and user experience, for instance by
     * not resolving some dependency's class/member links.
     *
     * When using offline mode, you can cache fetched files locally and provide them to
     * Dokka as local paths. For instance, see [ExternalDocumentationLinkBuilder].
     *
     * Default is `false`.
     */
    @Parameter(defaultValue = "${DokkaDefaults.offlineMode}")
    var offlineMode: Boolean = DokkaDefaults.offlineMode

    /**
     * [Kotlin language version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, the latest language version available to Dokka's embedded compiler will be used.
     */
    @Parameter
    var languageVersion: String? = null

    /**
     * [Kotlin API version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, it will be deduced from [languageVersion].
     */
    @Parameter
    var apiVersion: String? = null

    /**
     * Directories or individual files that should be suppressed, meaning declarations from them
     * will be not documented.
     */
    @Parameter
    var suppressedFiles: List<String> = emptyList()

    /**
     * Set of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations,
     * as well as if you want to exclude public declarations and only document internal API.
     *
     * Can be configured on per-package basis, see [PackageOptions.documentedVisibilities].
     *
     * Default is [DokkaConfiguration.Visibility.PUBLIC].
     */
    @Parameter(property = "visibility")
    var documentedVisibilities: Set<DokkaConfiguration.Visibility> = DokkaDefaults.documentedVisibilities
        // hack to set the default value for lists, didn't find any other safe way
        // maven seems to overwrite Kotlin's default initialization value, so it doesn't matter what you put there
        get() = field.ifEmpty { DokkaDefaults.documentedVisibilities }

    /**
     * Whether to fail documentation generation if Dokka has emitted a warning or an error.
     * Will wait until all errors and warnings have been emitted first.
     *
     * This setting works well with [reportUndocumented]
     *
     * Default is `false`.
     */
    @Parameter
    var failOnWarning: Boolean = DokkaDefaults.failOnWarning

    /**
     * Whether to suppress obvious functions.
     *
     * A function is considered to be obvious if it is:
     * - Inherited from `kotlin.Any`, `Kotlin.Enum`, `java.lang.Object` or `java.lang.Enum`,
     *   such as `equals`, `hashCode`, `toString`.
     * - Synthetic (generated by the compiler) and does not have any documentation, such as
     *   `dataClass.componentN` or `dataClass.copy`.
     *
     * Default is `true`
     */
    @Parameter(defaultValue = "${DokkaDefaults.suppressObviousFunctions}")
    var suppressObviousFunctions: Boolean = DokkaDefaults.suppressObviousFunctions

    /**
     * Whether to suppress inherited members that aren't explicitly overridden in a given class.
     *
     * Note: this can suppress functions such as `equals`/`hashCode`/`toString`, but cannot suppress
     * synthetic functions such as `dataClass.componentN` and `dataClass.copy`. Use [suppressObviousFunctions]
     * for that.
     *
     * Default is `false`.
     */
    @Parameter(defaultValue = "${DokkaDefaults.suppressInheritedMembers}")
    var suppressInheritedMembers: Boolean = DokkaDefaults.suppressInheritedMembers

    /**
     * Dokka plugins to be using during documentation generation.
     *
     * Example:
     *
     * ```xml
     * <dokkaPlugins>
     *     <plugin>
     *         <groupId>org.jetbrains.dokka</groupId>
     *         <artifactId>gfm-plugin</artifactId>
     *         <version>1.7.20</version>
     *     </plugin>
     * </dokkaPlugins>
     * ```
     */
    @Parameter
    var dokkaPlugins: List<Dependency> = emptyList()
        get() = field + defaultDokkaPlugins

    @Parameter
    var cacheRoot: String? = null

    @Parameter
    var platform: String = ""

    /**
     * Deprecated. Use [documentedVisibilities] instead.
     */
    @Parameter
    var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic

    protected abstract fun getOutDir(): String

    override fun execute() {
        if (skip) {
            log.info("Dokka skip parameter is true so no dokka output will be produced")
            return
        }

        sourceLinks.forEach {
            if (it.path.contains("\\")) {
                throw MojoExecutionException("Incorrect path property, only Unix based path allowed.")
            }
        }

        fun defaultLinks(config: DokkaSourceSetImpl): Set<ExternalDocumentationLinkImpl> {
            val links = mutableSetOf<ExternalDocumentationLinkImpl>()
            if (!config.noJdkLink)
                links += ExternalDocumentationLink.jdk(jdkVersion)

            if (!config.noStdlibLink)
                links += ExternalDocumentationLink.kotlinStdlib()
            return links
        }

        val sourceSet = DokkaSourceSetImpl(
            displayName = displayName,
            sourceSetID = DokkaSourceSetID(moduleName, sourceSetName),
            classpath = classpath.map(::File),
            sourceRoots = sourceDirectories.map(::File).toSet(),
            dependentSourceSets = emptySet(),
            samples = samples.map(::File).toSet(),
            includes = includes.map(::File).toSet(),
            includeNonPublic = includeNonPublic,
            documentedVisibilities = documentedVisibilities,
            reportUndocumented = reportUndocumented,
            skipEmptyPackages = skipEmptyPackages,
            skipDeprecated = skipDeprecated,
            jdkVersion = jdkVersion,
            sourceLinks = sourceLinks.map { SourceLinkDefinitionImpl(it.path, URL(it.url), it.lineSuffix) }.toSet(),
            perPackageOptions = perPackageOptions.map {
                @Suppress("DEPRECATION") // for includeNonPublic, preserve backwards compatibility
                PackageOptionsImpl(
                    matchingRegex = it.matchingRegex,
                    includeNonPublic = it.includeNonPublic,
                    documentedVisibilities = it.documentedVisibilities,
                    reportUndocumented = it.reportUndocumented,
                    skipDeprecated = it.skipDeprecated,
                    suppress = it.suppress
                )
            },
            externalDocumentationLinks = externalDocumentationLinks.map { it.build() }.toSet(),
            languageVersion = languageVersion,
            apiVersion = apiVersion,
            noStdlibLink = noStdlibLink,
            noJdkLink = noJdkLink,
            suppressedFiles = suppressedFiles.map(::File).toSet(),
            analysisPlatform = if (platform.isNotEmpty()) Platform.fromString(platform) else Platform.DEFAULT,
        ).let {
            it.copy(
                externalDocumentationLinks = defaultLinks(it) + it.externalDocumentationLinks
            )
        }

        val logger = MavenDokkaLogger(log)

        val pluginsConfiguration =
            (mavenProject?.getPlugin("org.jetbrains.dokka:dokka-maven-plugin")?.configuration as? Xpp3Dom)
                ?.getChild("pluginsConfiguration")?.children?.map {
                    PluginConfigurationImpl(
                        it.name,
                        DokkaConfiguration.SerializationFormat.XML,
                        it.toString()
                    )
                }.orEmpty()

        val configuration = DokkaConfigurationImpl(
            moduleName = moduleName,
            outputDir = File(getOutDir()),
            offlineMode = offlineMode,
            cacheRoot = cacheRoot?.let(::File),
            sourceSets = listOf(sourceSet),
            pluginsClasspath = getArtifactByMaven("org.jetbrains.dokka", "dokka-analysis", dokkaVersion) + // compileOnly in base plugin
                    getArtifactByMaven("org.jetbrains.dokka", "dokka-base", dokkaVersion) +
                    dokkaPlugins.map { getArtifactByMaven(it.groupId, it.artifactId, it.version ?: dokkaVersion) }
                        .flatten(),
            pluginsConfiguration = pluginsConfiguration.toMutableList(),
            modules = emptyList(),
            failOnWarning = failOnWarning,
            suppressObviousFunctions = suppressObviousFunctions,
            suppressInheritedMembers = suppressInheritedMembers,
            // looks like maven has different life cycle compared to gradle,
            // so finalizing coroutines after each module pass causes an error.
            // see https://github.com/Kotlin/dokka/issues/2457
            finalizeCoroutines = false,
        )

        val gen = DokkaGenerator(configuration, logger)

        gen.generate()
    }

    private fun getArtifactByMaven(
        groupId: String,
        artifactId: String,
        version: String
    ): List<File> {

        val request = ArtifactResolutionRequest().apply {
            isResolveRoot = true
            isResolveTransitively = true
            localRepository = session!!.localRepository
            remoteRepositories = mavenProject!!.pluginArtifactRepositories
            isOffline = session!!.isOffline
            isForceUpdate = session!!.request.isUpdateSnapshots
            servers = session!!.request.servers
            mirrors = session!!.request.mirrors
            proxies = session!!.request.proxies
            artifact = DefaultArtifact(
                groupId, artifactId, version, "compile", "jar", null,
                DefaultArtifactHandler("jar")
            )
        }

        log.debug("Resolving $groupId:$artifactId:$version ...")

        val result: ArtifactResolutionResult = repositorySystem!!.resolve(request)
        resolutionErrorHandler!!.throwErrors(request, result)
        return result.artifacts.map { it.file }
    }

    private val dokkaVersion: String by lazy {
        mavenProject?.pluginArtifacts?.firstOrNull { it.groupId == "org.jetbrains.dokka" && it.artifactId == "dokka-maven-plugin" }?.version
            ?: throw IllegalStateException("Not found dokka plugin")
    }
}

@Mojo(
    name = "dokka",
    defaultPhase = LifecyclePhase.PRE_SITE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true
)
class DokkaMojo : AbstractDokkaMojo(emptyList()) {

    /**
     * Directory to which documentation will be generated.
     *
     * Default is `{project.basedir}/target/dokka`.
     */
    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokka")
    var outputDir: String = ""

    override fun getOutDir() = outputDir
}

@Mojo(
    name = "javadoc",
    defaultPhase = LifecyclePhase.PRE_SITE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true
)
class DokkaJavadocMojo : AbstractDokkaMojo(listOf(javadocDependency)) {

    /**
     * Directory to which documentation will be generated.
     *
     * Default is `{project.basedir}/target/dokkaJavadoc`.
     */
    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokkaJavadoc")
    var outputDir: String = ""

    override fun getOutDir() = outputDir
}

@Mojo(
    name = "javadocJar",
    defaultPhase = LifecyclePhase.PRE_SITE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true
)
class DokkaJavadocJarMojo : AbstractDokkaMojo(listOf(javadocDependency)) {

    /**
     * Directory to which documentation jar will be generated.
     *
     * Default is `{project.basedir}/target/dokkaJavadocJar`.
     */
    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokkaJavadocJar")
    var outputDir: String = ""

    /**
     * Specifies the directory where the generated jar file will be put.
     */
    @Parameter(property = "project.build.directory")
    private var jarOutputDirectory: String? = null

    /**
     * Specifies the filename that will be used for the generated jar file. Please note that `-javadoc`
     * or `-test-javadoc` will be appended to the file name.
     */
    @Parameter(property = "project.build.finalName")
    private var finalName: String? = null

    /**
     * Specifies whether to attach the generated artifact to the project helper.
     */
    @Parameter(property = "attach", defaultValue = "true")
    private val attach: Boolean = false

    /**
     * The archive configuration to use.
     * See [Maven Archiver Reference](https://maven.apache.org/shared/maven-archiver/index.html)
     */
    @Parameter
    private val archive = MavenArchiveConfiguration()

    @Parameter(property = "maven.javadoc.classifier", defaultValue = "javadoc", required = true)
    private var classifier: String? = null

    @Component
    private var projectHelper: MavenProjectHelper? = null

    @Component(role = Archiver::class, hint = "jar")
    private var jarArchiver: JarArchiver? = null

    override fun getOutDir() = outputDir

    override fun execute() {
        super.execute()
        if (!File(outputDir).exists()) {
            log.warn("No javadoc generated so no javadoc jar will be generated")
            return
        }
        val outputFile = generateArchive("$finalName-$classifier.jar")
        if (attach) {
            projectHelper?.attachArtifact(mavenProject, "javadoc", classifier, outputFile)
        }
    }

    private fun generateArchive(jarFileName: String): File {
        val javadocJar = File(jarOutputDirectory, jarFileName)

        val archiver = MavenArchiver()
        archiver.archiver = jarArchiver
        archiver.setOutputFile(javadocJar)
        archiver.archiver.addFileSet(DefaultFileSet().apply { directory = File(outputDir) })

        archive.isAddMavenDescriptor = false
        archiver.createArchive(session, mavenProject, archive)

        return javadocJar
    }
}

private val javadocDependency = Dependency().apply {
    groupId = "org.jetbrains.dokka"
    artifactId = "javadoc-plugin"
}
