package org.jetbrains.dokka.maven

import org.apache.maven.archiver.MavenArchiveConfiguration
import org.apache.maven.archiver.MavenArchiver
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.*
import org.apache.maven.project.MavenProject
import org.apache.maven.project.MavenProjectHelper
import org.codehaus.plexus.archiver.Archiver
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

class SourceLinkMapItem {
    @Parameter(name = "dir", required = true)
    var dir: String = ""

    @Parameter(name = "url", required = true)
    var url: String = ""

    @Parameter(name = "urlSuffix")
    var urlSuffix: String? = null
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

        @Parameter
        override var platforms: List<String> = emptyList()
    }

    class PackageOptions : DokkaConfiguration.PackageOptions {
        @Parameter
        override var prefix: String = ""
        @Parameter
        override var includeNonPublic: Boolean = false
        @Parameter
        override var reportUndocumented: Boolean = true
        @Parameter
        override var skipDeprecated: Boolean = false
        @Parameter
        override var suppress: Boolean = false
    }

    @Parameter(required = true, defaultValue = "\${project.compileSourceRoots}")
    var sourceDirectories: List<String> = emptyList()

    @Parameter
    var sourceRoots: List<SourceRoot> = emptyList()

    @Parameter
    var samplesDirs: List<String> = emptyList()

    @Parameter
    @Deprecated("Use <includes> instead")
    var includeDirs: List<String> = emptyList()

    @Parameter
    var includes: List<String> = emptyList()

    @Parameter(required = true, defaultValue = "\${project.compileClasspathElements}")
    var classpath: List<String> = emptyList()

    @Parameter
    var sourceLinks: Array<SourceLinkMapItem> = emptyArray()

    @Parameter(required = true, defaultValue = "\${project.artifactId}")
    var moduleName: String = ""

    @Parameter(required = false, defaultValue = "false")
    var skip: Boolean = false

    @Parameter(required = false, defaultValue = "6")
    var jdkVersion: Int = 6

    @Parameter
    var skipDeprecated = false
    @Parameter
    var skipEmptyPackages = true
    @Parameter
    var reportNotDocumented = true

    @Parameter
    var impliedPlatforms: List<String> = emptyList()

    @Parameter
    var perPackageOptions: List<PackageOptions> = emptyList()

    @Parameter
    var externalDocumentationLinks: List<ExternalDocumentationLinkBuilder> = emptyList()

    @Parameter(defaultValue = "false")
    var noStdlibLink: Boolean = false

    @Parameter(defaultValue = "false")
    var noJdkLink: Boolean = false

    @Parameter
    var cacheRoot: String? = null

    @Parameter
    var languageVersion: String? = null

    @Parameter
    var apiVersion: String? = null

    protected abstract fun getOutDir(): String
    protected abstract fun getOutFormat(): String

    override fun execute() {
        if (skip) {
            log.info("Dokka skip parameter is true so no dokka output will be produced")
            return
        }

        val gen = DokkaGenerator(
                MavenDokkaLogger(log),
                classpath,
                sourceDirectories.map { SourceRootImpl(it) } + sourceRoots,
                samplesDirs,
                includeDirs + includes,
                moduleName,
                DocumentationOptions(getOutDir(), getOutFormat(),
                        sourceLinks = sourceLinks.map { SourceLinkDefinitionImpl(it.dir, it.url, it.urlSuffix) },
                        jdkVersion = jdkVersion,
                        skipDeprecated = skipDeprecated,
                        skipEmptyPackages = skipEmptyPackages,
                        reportUndocumented = reportNotDocumented,
                        impliedPlatforms = impliedPlatforms,
                        perPackageOptions = perPackageOptions,
                        externalDocumentationLinks = externalDocumentationLinks.map { it.build() },
                        noStdlibLink = noStdlibLink,
                        noJdkLink = noJdkLink,
                        cacheRoot = cacheRoot,
                        languageVersion = languageVersion,
                        apiVersion = apiVersion
                )
        )

        gen.generate()
    }
}

@Mojo(name = "dokka", defaultPhase = LifecyclePhase.PRE_SITE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
class DokkaMojo : AbstractDokkaMojo() {
    @Parameter(required = true, defaultValue = "html")
    var outputFormat: String = "html"

    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokka")
    var outputDir: String = ""

    override fun getOutFormat() = outputFormat
    override fun getOutDir() = outputDir
}

@Mojo(name = "javadoc", defaultPhase = LifecyclePhase.PRE_SITE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
class DokkaJavadocMojo : AbstractDokkaMojo() {
    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokkaJavadoc")
    var outputDir: String = ""

    override fun getOutFormat() = "javadoc"
    override fun getOutDir() = outputDir
}

@Mojo(name = "javadocJar", defaultPhase = LifecyclePhase.PRE_SITE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
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
     * See [Maven Archiver Reference](http://maven.apache.org/shared/maven-archiver/index.html)
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
        if(!File(outputDir).exists()) {
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
        archiver.setArchiver(jarArchiver)
        archiver.setOutputFile(javadocJar)
        archiver.archiver.addDirectory(File(outputDir), arrayOf("**/**"), arrayOf())

        archive.setAddMavenDescriptor(false)
        archiver.createArchive(session, project, archive)

        return javadocJar
    }
}

