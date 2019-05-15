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

class AntSourceRoot(var path: String? = null) {
    fun toSourceRoot(): SourceRootImpl? = path?.let { path ->
        SourceRootImpl(path)
    }
}

class TextProperty(var value: String = "")

class AntPassConfig(task: Task) : DokkaConfiguration.PassConfiguration {
    override var moduleName: String = ""
    override val classpath: List<String>
        get() = buildClassPath.list().toList()

    override val sourceRoots: List<DokkaConfiguration.SourceRoot>
        get() = sourcePath.list().map { SourceRootImpl(it) } + antSourceRoots.mapNotNull { it.toSourceRoot() }

    override val samples: List<String>
        get() = samplesPath.list().toList()
    override val includes: List<String>
        get() = includesPath.list().toList()
    override var includeNonPublic: Boolean = false
    override var includeRootPackage: Boolean = true
    override var reportUndocumented: Boolean = false
    override var skipEmptyPackages: Boolean = true
    override var skipDeprecated: Boolean = false
    override var jdkVersion: Int = 6
    override val sourceLinks: List<DokkaConfiguration.SourceLinkDefinition>
        get() = antSourceLinkDefinition.map {
            val path = it.path!!
            val url = it.url!!
            SourceLinkDefinitionImpl(File(path).canonicalFile.absolutePath, url, it.lineSuffix)
        }
    override val perPackageOptions: MutableList<DokkaConfiguration.PackageOptions> = mutableListOf()
    override val externalDocumentationLinks: List<ExternalDocumentationLink>
        get() = buildExternalLinksBuilders.map { it.build() } + defaultExternalDocumentationLinks

    override var languageVersion: String? = null
    override var apiVersion: String? = null
    override var noStdlibLink: Boolean = false
    override var noJdkLink: Boolean = false
    override var suppressedFiles: MutableList<String> = mutableListOf()
    override var collectInheritedExtensionsFromLibraries: Boolean = false
    override var analysisPlatform: Platform = Platform.DEFAULT
    override var targets: List<String> = listOf()
        get() = buildTargets.filter { it.value != "" }
            .map { it.value }

    override var sinceKotlin: String? = null

    private val samplesPath: Path by lazy { Path(task.project) }
    private val includesPath: Path by lazy { Path(task.project) }
    private val buildClassPath: Path by lazy { Path(task.project) }
    val sourcePath: Path by lazy { Path(task.project) }
    val antSourceRoots: MutableList<AntSourceRoot> = mutableListOf()

    private val buildTargets: MutableList<TextProperty> = mutableListOf()
    private val buildExternalLinksBuilders: MutableList<ExternalDocumentationLink.Builder> = mutableListOf()
    val antSourceLinkDefinition: MutableList<AntSourceLinkDefinition> = mutableListOf()

    private val defaultExternalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink>
        get() {
            val links = mutableListOf<DokkaConfiguration.ExternalDocumentationLink>()
            if (!noJdkLink)
                links += DokkaConfiguration.ExternalDocumentationLink.Builder("https://docs.oracle.com/javase/$jdkVersion/docs/api/").build()

            if (!noStdlibLink)
                links += DokkaConfiguration.ExternalDocumentationLink.Builder("https://kotlinlang.org/api/latest/jvm/stdlib/").build()
            return links
        }


    fun setSamples(ref: Path) {
        samplesPath.append(ref)
    }

    fun setSamplesRef(ref: Reference) {
        samplesPath.createPath().refid = ref
    }

    fun setInclude(ref: Path) {
        includesPath.append(ref)
    }

    fun setClasspath(classpath: Path) {
        buildClassPath.append(classpath)
    }

    fun createPackageOptions(): AntPackageOptions = AntPackageOptions().apply { perPackageOptions.add(this) }

    fun createSourceRoot(): AntSourceRoot = AntSourceRoot().apply {  antSourceRoots.add(this) }

    fun createTarget(): TextProperty = TextProperty().apply {
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
        antSourceLinkDefinition.add(def)
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
        get() = buildImpliedPlatforms.map { it.value }.toList()
    private val buildImpliedPlatforms: MutableList<TextProperty> = mutableListOf()

    override var cacheRoot: String? = null
    override val passesConfigurations: MutableList<AntPassConfig> = mutableListOf()

    fun createPassConfig() = AntPassConfig(this).apply { passesConfigurations.add(this) }
    fun createImpliedPlatform(): TextProperty = TextProperty().apply { buildImpliedPlatforms.add(this) }


    override fun execute() {
        for (passConfig in passesConfigurations) {
            if (passConfig.sourcePath.list().isEmpty() && passConfig.antSourceRoots.isEmpty()) {
                throw BuildException("At least one source path needs to be specified")
            }

            if (passConfig.moduleName == "") {
                throw BuildException("Module name needs to be specified and not empty")
            }

            for (sourceLink in passConfig.antSourceLinkDefinition) {
                if (sourceLink.path == null) {
                    throw BuildException("'path' attribute of a <sourceLink> element is required")
                }
                if (sourceLink.path!!.contains("\\")) {
                    throw BuildException("'dir' attribute of a <sourceLink> - incorrect value, only Unix based path allowed")
                }

                if (sourceLink.url == null) {
                    throw BuildException("'url' attribute of a <sourceLink> element is required")
                }
            }
        }

        if (outputDir == "") {
            throw BuildException("Output directory needs to be specified and not empty")
        }

        val generator = DokkaGenerator(this, AntLogger(this))
        generator.generate()
    }
}