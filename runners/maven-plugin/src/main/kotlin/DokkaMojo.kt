package org.jetbrains.dokka.maven

import org.apache.maven.archiver.MavenArchiveConfiguration
import org.apache.maven.archiver.MavenArchiver
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.*
import org.apache.maven.project.MavenProject
import org.apache.maven.project.MavenProjectHelper
import org.codehaus.plexus.archiver.Archiver
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

class SourceLinkMapItem : DokkaConfiguration.SourceLinkDefinition {
    @Parameter(name = "path", required = true)
    override var path: String = ""

    @Parameter(name = "url", required = true)
    override var url: String = ""

    @Parameter(name = "lineSuffix")
    override var lineSuffix: String? = null
}

class ExternalDocumentationLink : DokkaConfiguration.ExternalDocumentationLink {

    @Parameter(name = "url", required = true)
    override var url: URL = URL("")
    @Parameter(name = "packageListUrl", required = true)
    override var packageListUrl: URL = URL("")
}

abstract class AbstractDokkaMojo : AbstractMojo() {
    class SourceRoot : DokkaConfiguration.SourceRoot {
        @Parameter(required = true)
        override var path: String = ""
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

    class Multiplatform : DokkaConfiguration.PassConfiguration {
        @Parameter(required = true, defaultValue = "\${project.compileSourceRoots}")
        var sourceDirectories: List<String> = emptyList()

        @Parameter(required = true, defaultValue = "\${project.artifactId}")
        override var moduleName: String = ""

        @Parameter(required = true, defaultValue = "\${project.compileClasspathElements}")
        override var classpath: List<String> = emptyList()

        @Parameter
        override var sourceRoots: List<SourceRoot> = emptyList()

        @Parameter
        override val samples: List<String> = emptyList()

        @Parameter
        override val includes: List<String> = emptyList()

        @Parameter
        override val includeNonPublic: Boolean = false

        @Parameter
        override val includeRootPackage: Boolean = false

        @Parameter
        override val reportUndocumented: Boolean = true
        @Parameter
        override val skipEmptyPackages: Boolean = true

        @Parameter
        override val skipDeprecated: Boolean = false

        @Parameter(required = false, defaultValue = "6")
        override var jdkVersion: Int = 6

        @Parameter
        override val sourceLinks: List<SourceLinkMapItem> = emptyList()

        @Parameter
        override val perPackageOptions: List<PackageOptions> = emptyList()

        @Parameter
        override val externalDocumentationLinks: List<ExternalDocumentationLink> = emptyList()

        @Parameter
        override val languageVersion: String? = null

        @Parameter
        override val apiVersion: String? = null

        @Parameter(defaultValue = "false")
        override var noStdlibLink: Boolean = false

        @Parameter(defaultValue = "false")
        override var noJdkLink: Boolean = false

        @Parameter
        override val suppressedFiles: List<String>  = emptyList()

        @Parameter
        override val collectInheritedExtensionsFromLibraries: Boolean  = false

        override var analysisPlatform: Platform = Platform.DEFAULT

        @Parameter
        val platform: String = ""

        @Parameter
        override val targets: List<String> = emptyList()

        @Parameter
        override val sinceKotlin: String = "1.0"
    }

    @Parameter
    var multiplatform: List<Multiplatform> = emptyList()

    @Parameter
    var config: Multiplatform? = null

    @Parameter(required = true, defaultValue = "\${project.artifactId}")
    var moduleName: String = ""

    @Parameter
    var impliedPlatforms: List<String> = emptyList() //TODO check

    @Parameter
    var cacheRoot: String? = null

    protected abstract fun getOutDir(): String

    protected abstract fun getOutFormat(): String

    @Parameter(required = false, defaultValue = "false")
    var skip: Boolean = false

    @Parameter(required = true, defaultValue = "\${project.compileClasspathElements}")
    var classpath: List<String> = emptyList()

    //todo remove
    @Parameter(required = true, defaultValue = "\${project.compileSourceRoots}")
    var sourceDirectories: List<String> = emptyList()
    @Parameter(required = false, defaultValue = "6")
     val jdkVersion: Int = 6
    @Parameter(defaultValue = "false")
     val noStdlibLink: Boolean = false
    @Parameter(defaultValue = "false")
     val noJdkLink: Boolean = false

    override fun execute() {
        if (skip) {
            log.info("Dokka skip parameter is true so no dokka output will be produced")
            return
        }

        val passConfigurationList = (
                if (multiplatform.isEmpty() && config != null) listOf(config!!) else multiplatform
                ).map {
            defaultPassConfiguration(it)
        }

        passConfigurationList.flatMap { it.sourceLinks }.forEach {
            if (it.path.contains("\\")) {
                throw MojoExecutionException("Incorrect path property, only Unix based path allowed.")
            }
        }

        val platforms = impliedPlatforms
        val cacheRoot = cacheRoot

        val configuration = object : DokkaConfiguration {
            override val outputDir = getOutDir()
            override val format = getOutFormat()
            override val impliedPlatforms = platforms
            override val cacheRoot = cacheRoot
            override val passesConfigurations = passConfigurationList
            override val generateIndexPages = false // TODO: Should we implement this?
        }

        val gen = DokkaGenerator(configuration, MavenDokkaLogger(log))

        gen.generate()
    }

    private fun defaultPassConfiguration(passConfig: Multiplatform): Multiplatform {
        passConfig.moduleName = moduleName
        passConfig.classpath = classpath
        passConfig.externalDocumentationLinks.map {
            val builder = DokkaConfiguration.ExternalDocumentationLink.Builder(it.url, it.packageListUrl)
            builder.build()
        }
        if(passConfig.platform.isNotEmpty()){
            passConfig.analysisPlatform = Platform.fromString(passConfig.platform)
        }
        // todo fix
        passConfig.sourceRoots = sourceDirectories.map {
            val sourceRoot = SourceRoot()
            sourceRoot.path = it
            sourceRoot
        } + passConfig.sourceRoots
        passConfig.jdkVersion = jdkVersion
        passConfig.noStdlibLink = noStdlibLink
        passConfig.noJdkLink = noJdkLink
//        passConfig.sourceRoots = passConfig.sourceDirectories.map {
//            val sourceRoot = SourceRoot()
//            sourceRoot.path = it
//            sourceRoot
//        } + passConfig.sourceRoots

        return passConfig
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

