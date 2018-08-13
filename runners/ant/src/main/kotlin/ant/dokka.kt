package org.jetbrains.dokka.ant

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.apache.tools.ant.Task
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.Reference
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.io.File
import java.io.IOException

class AntLogger(val task: Task): DokkaLogger {
    override fun info(message: String) = task.log(message, Project.MSG_INFO)
    override fun warn(message: String) = task.log(message, Project.MSG_WARN)
    override fun error(message: String) = task.log(message, Project.MSG_ERR)
}

class AntSourceLinkDefinition(var path: String? = null, var url: String? = null, var lineSuffix: String? = null)

class AntSourceRoot(var path: String? = null, var platforms: String? = null,
                    var platform: Platform = Platform.DEFAULT) {
    fun toSourceRoot(): SourceRootImpl? = path?.let {
        path ->
        SourceRootImpl(path, platforms?.split(',').orEmpty(), platform)
    }
}

class BuildTarget(var name: String = "")

class BuildPlatform(var name: String = "")

class AntPassConfig(task: Task) : DokkaConfiguration.PassConfiguration {
    override var moduleName: String = ""
    override val classpath: List<String>
        get() = buildClassPath.list().toList()

    override val sourceRoots: List<DokkaConfiguration.SourceRoot>
        get() = sourcePath.list().map { SourceRootImpl(it) } + rawSourceRoots

    override val samples: List<String>
        get() = samplesPath.list().toList()
    override val includes: List<String>
        get() = includesPath.list().toList()
    override var includeNonPublic: Boolean = false
    override var includeRootPackage: Boolean = false
    override var reportUndocumented: Boolean = false
    override var skipEmptyPackages: Boolean = false
    override var skipDeprecated: Boolean = false
    override var jdkVersion: Int = 6
    override val sourceLinks: List<DokkaConfiguration.SourceLinkDefinition>
        get() = buildAntSourceLinkDefinition.map {
            val path = it.path ?: throw BuildException("'path' attribute of a <sourceLink> element is required")
            val url = it.url ?: throw BuildException("'url' attribute of a <sourceLink> element is required")
            SourceLinkDefinitionImpl(File(path).canonicalFile.absolutePath, url, it.lineSuffix)
        }
    override val perPackageOptions: MutableList<DokkaConfiguration.PackageOptions> = mutableListOf()
    override val externalDocumentationLinks: List<ExternalDocumentationLink>
        get() = buildExternalLinksBuilders.map { it.build() }

    override var languageVersion: String? = null
    override var apiVersion: String? = null
    override var noStdlibLink: Boolean = false
    override var noJdkLink: Boolean = false
    override var suppressedFiles: MutableList<String> = mutableListOf()
    override var collectInheritedExtensionsFromLibraries: Boolean = false
    override var analysisPlatform: Platform = Platform.DEFAULT
    override var targets: List<String> = listOf()
        get() = buildTargets.filter { it.name != "" }
            .map { it.name }

    private val samplesPath: Path by lazy { Path(task.project) }
    private val includesPath: Path by lazy { Path(task.project) }
    private val buildClassPath: Path by lazy { Path(task.project) }
    private val sourcePath: Path by lazy { Path(task.project) }
    private val rawSourceRoots: MutableList<SourceRootImpl> = mutableListOf()

    private val buildTargets: MutableList<BuildTarget> = mutableListOf()
    private val buildExternalLinksBuilders: MutableList<ExternalDocumentationLink.Builder> = mutableListOf()
    private val buildAntSourceLinkDefinition: MutableList<AntSourceLinkDefinition> = mutableListOf()

    fun setSamples(ref: Reference) {
        samplesPath.createPath().refid = ref
    }

    fun setSamplesRef(ref: Reference) {
        samplesPath.createPath().refid = ref
    }

    fun setInclude(ref: Reference) {
        includesPath.createPath().refid = ref
    }

    fun setClasspath(classpath: Path) {
        buildClassPath.append(classpath)
    }

    fun createPackageOptions(): AntPackageOptions = AntPackageOptions().apply { perPackageOptions.add(this) }

    fun createSourceRoot(): AntSourceRoot = AntSourceRoot().apply { this.toSourceRoot()?.let { rawSourceRoots.add(it) } }

    fun createTarget(): BuildTarget = BuildTarget().apply {
            buildTargets.add(this)
    }

    fun setClasspathRef(ref: Reference) {
        buildClassPath.createPath().refid = ref
    }

    fun setSrc(src: Path) {
        sourcePath.append(src)
    }

    fun setSrcRef(ref: Reference) {
        sourcePath.createPath().refid = ref
    }

    fun createSourceLink(): AntSourceLinkDefinition {
        val def = AntSourceLinkDefinition()
        buildAntSourceLinkDefinition.add(def)
        return def
    }

    fun createExternalDocumentationLink() =
        ExternalDocumentationLink.Builder().apply { buildExternalLinksBuilders.add(this) }

}

class AntPackageOptions(
        override var prefix: String = "",
        override var includeNonPublic: Boolean = false,
        override var reportUndocumented: Boolean = true,
        override var skipDeprecated: Boolean = false,
        override var suppress: Boolean = false) : DokkaConfiguration.PackageOptions

class DokkaAntTask: Task(), DokkaConfiguration {

    override var format: String = "html"
    override var generateIndexPages: Boolean = false
    override var outputDir: String = ""
    override var impliedPlatforms: List<String> = listOf()
        get() = buildImpliedPlatforms.map { it.name }.toList()
    private val buildImpliedPlatforms: MutableList<BuildPlatform> = mutableListOf()

    override var cacheRoot: String? = null
    override val passesConfigurations: MutableList<AntPassConfig> = mutableListOf()

    fun createPassConfig() = AntPassConfig(this).apply { passesConfigurations.add(this) }
    fun createImpliedPlatform(): BuildPlatform = BuildPlatform().apply { buildImpliedPlatforms.add(this) }


    override fun execute() {

        throw IOException(passesConfigurations.flatMap { it.targets }.joinToString())
//        if (sourcePath.list().isEmpty() && antSourceRoots.isEmpty()) {
//            throw BuildException("At least one source path needs to be specified")
//        }
//        if (moduleName == null) {
//            throw BuildException("Module name needs to be specified")
//        }
//        if (outputDir == null) {
//            throw BuildException("Output directory needs to be specified")
//        }
//        val sourceLinks = antSourceLinks.map {
//            val path = it.path ?: throw BuildException("'path' attribute of a <sourceLink> element is required")
//            val url = it.url ?: throw BuildException("'url' attribute of a <sourceLink> element is required")
//            SourceLinkDefinitionImpl(File(path).canonicalFile.absolutePath, url, it.lineSuffix)
//        }

//        val passConfiguration = PassConfigurationImpl(
//            classpath = compileClasspath.list().toList(),
//            sourceRoots = sourcePath.list().map { SourceRootImpl(it) } + antSourceRoots.mapNotNull { it.toSourceRoot() },
//            samples = samplesPath.list().toList(),
//            includes = includesPath.list().toList(),
//            moduleName = moduleName!!,
//            skipDeprecated = skipDeprecated,
//            sourceLinks = sourceLinks,
//            jdkVersion = jdkVersion,
//            perPackageOptions = antPackageOptions,
//            externalDocumentationLinks = antExternalDocumentationLinks.map { it.build() },
//            noStdlibLink = noStdlibLink,
//            noJdkLink = noJdkLink,
//            languageVersion = languageVersion,
//            apiVersion = apiVersion
//        )
//
//        val configuration = DokkaConfigurationImpl(
//            outputDir = outputDir!!,
//            format = outputFormat,
//            impliedPlatforms = impliedPlatforms.split(','),
//            cacheRoot = cacheRoot,
//            passesConfigurations = listOf(
//                passConfiguration
//            )
//        )

        val generator = DokkaGenerator(this, AntLogger(this))
        generator.generate()
    }
}