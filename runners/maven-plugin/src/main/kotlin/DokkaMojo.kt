package org.jetbrains.dokka.maven

import org.apache.maven.archiver.MavenArchiveConfiguration
import org.apache.maven.archiver.MavenArchiver
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.*
import org.apache.maven.project.MavenProject
import org.apache.maven.project.MavenProjectHelper
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.codehaus.plexus.archiver.Archiver
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

class SourceLinkMapItem {
    @Parameter(name = "path", required = true)
    var path: String = ""

    @Parameter(name = "url", required = true)
    var url: String = ""

    @Parameter(name = "lineSuffix")
    var lineSuffix: String? = null
}

class ExternalDocumentationLinkBuilder : DokkaConfiguration.ExternalDocumentationLink.Builder() {

    @Parameter(name = "url", required = true)
    override var url: URL? = null

    @Parameter(name = "packageListUrl", required = true)
    override var packageListUrl: URL? = null
}

abstract class AbstractDokkaMojo : AbstractMojo() {
    class SourceRoot : DokkaConfiguration.SourceRoot {
        @Parameter(required = true)
        override var path: String = ""
    }

    @Parameter(defaultValue = "\${project}", readonly = true)
    private var mavenProject: MavenProject? = null

    @Parameter()
    private var session: RepositorySystemSession? = null

    class PackageOptions : DokkaConfiguration.PackageOptions {
        @Parameter
        override var prefix: String = ""

        @Parameter
        override var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic

        @Parameter
        override var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

        @Parameter
        override var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

        @Parameter
        override var suppress: Boolean = DokkaDefaults.suppress
    }

    @Parameter(required = true, defaultValue = "\${project.sourceSet}")
    var sourceSetName: String = ""

    @Parameter(required = true, defaultValue = "\${project.compileSourceRoots}")
    var sourceDirectories: List<String> = emptyList()

    @Parameter
    var sourceRoots: List<SourceRoot> = emptyList()

    @Parameter
    var samples: List<String> = emptyList()

    @Parameter
    var includes: List<String> = emptyList()

    @Parameter(required = true, defaultValue = "\${project.compileClasspathElements}")
    var classpath: List<String> = emptyList()

    @Parameter
    var sourceLinks: List<SourceLinkMapItem> = emptyList()

    @Parameter(required = true, defaultValue = "\${project.artifactId}")
    var moduleName: String = ""

    @Parameter
    var moduleDisplayName: String = ""

    @Parameter(required = false, defaultValue = "false")
    var skip: Boolean = false

    @Parameter(required = false, defaultValue = "${DokkaDefaults.jdkVersion}")
    var jdkVersion: Int = DokkaDefaults.jdkVersion

    @Parameter
    var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

    @Parameter
    var skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages

    @Parameter
    var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

    @Parameter
    var impliedPlatforms: List<String> = emptyList()

    @Parameter
    var perPackageOptions: List<PackageOptions> = emptyList()

    @Parameter
    var externalDocumentationLinks: List<ExternalDocumentationLinkBuilder> = emptyList()

    @Parameter(defaultValue = "${DokkaDefaults.noStdlibLink}")
    var noStdlibLink: Boolean = DokkaDefaults.noStdlibLink

    @Parameter(defaultValue = "${DokkaDefaults.noJdkLink}")
    var noJdkLink: Boolean = DokkaDefaults.noJdkLink

    @Parameter
    var cacheRoot: String? = null

    @Parameter(defaultValue = "JVM")
    var displayName: String = "JVM"

    @Parameter(defaultValue = "${DokkaDefaults.offlineMode}")
    var offlineMode: Boolean = DokkaDefaults.offlineMode

    @Parameter
    var languageVersion: String? = null

    @Parameter
    var apiVersion: String? = null

    @Parameter
    var includeRootPackage: Boolean = DokkaDefaults.includeRootPackage

    @Parameter
    var suppressedFiles: List<String> = emptyList()

    @Parameter
    var platform: String = ""

    @Parameter
    var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic

    @Parameter
    var failOnWarning: Boolean = DokkaDefaults.failOnWarning

    @Parameter
    var dokkaPlugins: List<Dependency> = emptyList()

    protected abstract fun getOutDir(): String

    protected abstract fun getOutFormat(): String

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
        fun defaultLinks(config: DokkaSourceSetImpl): List<ExternalDocumentationLinkImpl> {
            val links = mutableListOf<ExternalDocumentationLinkImpl>()
            if (!config.noJdkLink)
                links += DokkaConfiguration.ExternalDocumentationLink
                    .Builder("https://docs.oracle.com/javase/${config.jdkVersion}/docs/api/")
                    .build() as ExternalDocumentationLinkImpl

            if (!config.noStdlibLink)
                links += DokkaConfiguration.ExternalDocumentationLink
                    .Builder("https://kotlinlang.org/api/latest/jvm/stdlib/")
                    .build() as ExternalDocumentationLinkImpl
            return links
        }

        val sourceSet = DokkaSourceSetImpl(
            moduleDisplayName = moduleDisplayName.takeIf(String::isNotBlank) ?: moduleName,
            displayName = displayName,
            sourceSetID = DokkaSourceSetID(moduleName, sourceSetName),
            classpath = classpath,
            sourceRoots = sourceDirectories.map { SourceRootImpl(it) },
            dependentSourceSets = emptySet(),
            samples = samples,
            includes = includes,
            includeNonPublic = includeNonPublic,
            includeRootPackage = includeRootPackage,
            reportUndocumented = reportUndocumented,
            skipEmptyPackages = skipEmptyPackages,
            skipDeprecated = skipDeprecated,
            jdkVersion = jdkVersion,
            sourceLinks = sourceLinks.map { SourceLinkDefinitionImpl(it.path, it.url, it.lineSuffix) },
            perPackageOptions = perPackageOptions.map {
                PackageOptionsImpl(
                    prefix = it.prefix,
                    includeNonPublic = it.includeNonPublic,
                    reportUndocumented = it.reportUndocumented,
                    skipDeprecated = it.skipDeprecated,
                    suppress = it.suppress
                )
            },
            externalDocumentationLinks = externalDocumentationLinks.map { it.build() as ExternalDocumentationLinkImpl },
            languageVersion = languageVersion,
            apiVersion = apiVersion,
            noStdlibLink = noStdlibLink,
            noJdkLink = noJdkLink,
            suppressedFiles = suppressedFiles,
            analysisPlatform = if (platform.isNotEmpty()) Platform.fromString(platform) else Platform.DEFAULT
        ).let {
            it.copy(
                externalDocumentationLinks = defaultLinks(it) + it.externalDocumentationLinks
            )
        }

        val logger = MavenDokkaLogger(log)

        val configuration = DokkaConfigurationImpl(
            outputDir = getOutDir(),
            format = getOutFormat(),
            offlineMode = offlineMode,
            cacheRoot = cacheRoot,
            sourceSets = listOf(sourceSet).also {
                if (sourceSet.moduleDisplayName.isEmpty()) logger.warn("Not specified module name. It can result in unexpected behaviour while including documentation for module")
            },
            pluginsClasspath = getArtifactByAether("org.jetbrains.dokka", "dokka-base", dokkaVersion) +
                    dokkaPlugins.map { getArtifactByAether(it.groupId, it.artifactId, it.version) }.flatten(),
            pluginsConfiguration = mutableMapOf(), //TODO implement as it is in Gradle
            modules = emptyList(),
            failOnWarning = failOnWarning
        )

        val gen = DokkaGenerator(configuration, logger)

        gen.generate()
    }

    private fun newRepositorySystem(): RepositorySystem {
        val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        return locator.getService(RepositorySystem::class.java)
    }

    private fun newSession(system: RepositorySystem): RepositorySystemSession {
        val session: DefaultRepositorySystemSession =
            MavenRepositorySystemUtils.newSession()
        val localRepo = LocalRepository(System.getProperty("user.home") + "/.m2/repository")
        session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)
        return session
    }

    private fun getArtifactByAether(
        groupId: String,
        artifactId: String,
        version: String
    ): List<File> {
        val repoSystem: RepositorySystem = newRepositorySystem()
        val session: RepositorySystemSession = newSession(repoSystem)
        val dependency =
            org.eclipse.aether.graph.Dependency(DefaultArtifact("$groupId:$artifactId:$version"), "compile")
        val collectRequest = CollectRequest()
        collectRequest.root = dependency
        val repositories: List<RemoteRepository> =
            (mavenProject?.remoteProjectRepositories?.plus(mavenProject?.remotePluginRepositories ?: emptyList())
                ?: mavenProject?.remotePluginRepositories ?: emptyList())
        repositories.forEach {
            collectRequest.addRepository(
                RemoteRepository.Builder(
                    "repo",
                    "default",
                    it.url
                ).build()
            )
        }
        val node: DependencyNode = repoSystem.collectDependencies(session, collectRequest).root
        val dependencyRequest = DependencyRequest()
        dependencyRequest.root = node
        repoSystem.resolveDependencies(session, dependencyRequest)
        val nlg = PreorderNodeListGenerator()
        node.accept(nlg)
        return nlg.files
    }

    private val dokkaVersion: String by lazy {
        mavenProject?.pluginArtifacts?.filter { it.groupId == "org.jetbrains.dokka" && it.artifactId == "dokka-maven-plugin" }
            ?.firstOrNull()?.version ?: throw IllegalStateException("Not found dokka plugin")
    }
}

@Mojo(
    name = "dokka",
    defaultPhase = LifecyclePhase.PRE_SITE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true
)
class DokkaMojo : AbstractDokkaMojo() {
    @Parameter(required = true, defaultValue = "html")
    var outputFormat: String = "html"

    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokka")
    var outputDir: String = ""

    override fun getOutFormat() = outputFormat
    override fun getOutDir() = outputDir
}

@Mojo(
    name = "javadoc",
    defaultPhase = LifecyclePhase.PRE_SITE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true
)
class DokkaJavadocMojo : AbstractDokkaMojo() {
    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokkaJavadoc")
    var outputDir: String = ""

    override fun getOutFormat() = "javadoc"
    override fun getOutDir() = outputDir
}

@Mojo(
    name = "javadocJar",
    defaultPhase = LifecyclePhase.PRE_SITE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresProject = true
)
class DokkaJavadocJarMojo : AbstractDokkaMojo() {
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

    @Parameter(defaultValue = "\${session}", readonly = true, required = true)
    protected var session: MavenSession? = null

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected var project: MavenProject? = null

    @Component
    private var projectHelper: MavenProjectHelper? = null

    @Component(role = Archiver::class, hint = "jar")
    private var jarArchiver: JarArchiver? = null

    override fun getOutFormat() = "javadoc"
    override fun getOutDir() = outputDir

    override fun execute() {
        super.execute()
        if (!File(outputDir).exists()) {
            log.warn("No javadoc generated so no javadoc jar will be generated")
            return
        }
        val outputFile = generateArchive("$finalName-$classifier.jar")
        if (attach) {
            projectHelper?.attachArtifact(project, "javadoc", classifier, outputFile)
        }
    }

    private fun generateArchive(jarFileName: String): File {
        val javadocJar = File(jarOutputDirectory, jarFileName)

        val archiver = MavenArchiver()
        archiver.archiver = jarArchiver
        archiver.setOutputFile(javadocJar)
        archiver.archiver.addDirectory(File(outputDir), arrayOf("**/**"), arrayOf())

        archive.isAddMavenDescriptor = false
        archiver.createArchive(session, project, archive)

        return javadocJar
    }
}
