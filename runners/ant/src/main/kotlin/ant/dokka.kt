package org.jetbrains.dokka.ant

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.apache.tools.ant.Task
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.Reference
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.io.File

class AntLogger(val task: Task): DokkaLogger {
    override fun info(message: String) = task.log(message, Project.MSG_INFO)
    override fun warn(message: String) = task.log(message, Project.MSG_WARN)
    override fun error(message: String) = task.log(message, Project.MSG_ERR)
}

class AntSourceLinkDefinition(var path: String? = null, var url: String? = null, var lineSuffix: String? = null)

class AntSourceRoot(var path: String? = null, var platforms: String? = null) {
    fun toSourceRoot(): SourceRootImpl? = path?.let {
        path ->
        SourceRootImpl(path, platforms?.split(',').orEmpty())
    }
}

class AntPackageOptions(
        override var prefix: String = "",
        override var includeNonPublic: Boolean = false,
        override var reportUndocumented: Boolean = true,
        override var skipDeprecated: Boolean = false,
        override var suppress: Boolean = false) : DokkaConfiguration.PackageOptions


class DokkaAntTask: Task() {
    var moduleName: String? = null
    var outputDir: String? = null
    var outputFormat: String = "html"
    var impliedPlatforms: String = ""
    var jdkVersion: Int = 6

    var noStdlibLink: Boolean = false

    var skipDeprecated: Boolean = false

    var cacheRoot: String? = null

    var languageVersion: String? = null
    var apiVersion: String? = null

    val compileClasspath: Path by lazy { Path(getProject()) }
    val sourcePath: Path by lazy { Path(getProject()) }
    val samplesPath: Path by lazy { Path(getProject()) }
    val includesPath: Path by lazy { Path(getProject()) }

    val antSourceLinks: MutableList<AntSourceLinkDefinition> = arrayListOf()
    val antSourceRoots: MutableList<AntSourceRoot> = arrayListOf()
    val antPackageOptions: MutableList<AntPackageOptions> = arrayListOf()
    val antExternalDocumentationLinks = mutableListOf<ExternalDocumentationLink.Builder>()

    fun setClasspath(classpath: Path) {
        compileClasspath.append(classpath)
    }

    fun setClasspathRef(ref: Reference) {
        compileClasspath.createPath().refid = ref
    }

    fun setSrc(src: Path) {
        sourcePath.append(src)
    }

    fun setSrcRef(ref: Reference) {
        sourcePath.createPath().refid = ref
    }

    fun setSamples(samples: Path) {
        samplesPath.append(samples)
    }

    fun setSamplesRef(ref: Reference) {
        samplesPath.createPath().refid = ref
    }

    fun setInclude(include: Path) {
        includesPath.append(include)
    }

    fun createSourceLink(): AntSourceLinkDefinition {
        val def = AntSourceLinkDefinition()
        antSourceLinks.add(def)
        return def
    }

    fun createSourceRoot(): AntSourceRoot = AntSourceRoot().apply { antSourceRoots.add(this) }

    fun createPackageOptions(): AntPackageOptions = AntPackageOptions().apply { antPackageOptions.add(this) }

    fun createExternalDocumentationLink() = ExternalDocumentationLink.Builder().apply { antExternalDocumentationLinks.add(this) }

    override fun execute() {
        if (sourcePath.list().isEmpty() && antSourceRoots.isEmpty()) {
            throw BuildException("At least one source path needs to be specified")
        }
        if (moduleName == null) {
            throw BuildException("Module name needs to be specified")
        }
        if (outputDir == null) {
            throw BuildException("Output directory needs to be specified")
        }
        val sourceLinks = antSourceLinks.map {
            val path = it.path ?: throw BuildException("'path' attribute of a <sourceLink> element is required")
            val url = it.url ?: throw BuildException("'url' attribute of a <sourceLink> element is required")
            SourceLinkDefinitionImpl(File(path).canonicalFile.absolutePath, url, it.lineSuffix)
        }

        val generator = DokkaGenerator(
                AntLogger(this),
                compileClasspath.list().toList(),
                sourcePath.list().map { SourceRootImpl(it) } + antSourceRoots.mapNotNull { it.toSourceRoot() },
                samplesPath.list().toList(),
                includesPath.list().toList(),
                moduleName!!,
                DocumentationOptions(
                        outputDir!!,
                        outputFormat,
                        skipDeprecated = skipDeprecated,
                        sourceLinks = sourceLinks,
                        jdkVersion = jdkVersion,
                        impliedPlatforms = impliedPlatforms.split(','),
                        perPackageOptions = antPackageOptions,
                        externalDocumentationLinks = antExternalDocumentationLinks.map { it.build() },
                        noStdlibLink = noStdlibLink,
                        cacheRoot = cacheRoot,
                        languageVersion = languageVersion,
                        apiVersion = apiVersion
                )
        )
        generator.generate()
    }
}