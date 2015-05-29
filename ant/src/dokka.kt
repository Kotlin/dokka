package org.jetbrains.dokka.ant

import org.apache.tools.ant.Task
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.Reference
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.SourceLinkDefinition
import java.io.File

class AntLogger(val task: Task): DokkaLogger {
    override fun info(message: String) = task.log(message, Project.MSG_INFO)
    override fun warn(message: String) = task.log(message, Project.MSG_WARN)
    override fun error(message: String) = task.log(message, Project.MSG_ERR)
}

class AntSourceLinkDefinition(var path: String? = null, var url: String? = null, var lineSuffix: String? = null)

class DokkaAntTask(): Task() {
    public var moduleName: String? = null
    public var outputDir: String? = null
    public var outputFormat: String = "html"

    public var skipDeprecated: Boolean = false

    public val compileClasspath: Path = Path(getProject())
    public val sourcePath: Path = Path(getProject())
    public val samplesPath: Path = Path(getProject())
    public val includesPath: Path = Path(getProject())

    public val antSourceLinks: MutableList<AntSourceLinkDefinition> = arrayListOf()

    public fun setClasspath(classpath: Path) {
        compileClasspath.append(classpath)
    }

    public fun setClasspathRef(ref: Reference) {
        compileClasspath.createPath().setRefid(ref)
    }

    public fun setSrc(src: Path) {
        sourcePath.append(src)
    }

    public fun setSrcRef(ref: Reference) {
        sourcePath.createPath().setRefid(ref)
    }

    public fun setSamples(samples: Path) {
        samplesPath.append(samples)
    }

    public fun setSamplesRef(ref: Reference) {
        samplesPath.createPath().setRefid(ref)
    }

    public fun setInclude(include: Path) {
        includesPath.append(include)
    }

    public fun createSourceLink(): AntSourceLinkDefinition {
        val def = AntSourceLinkDefinition()
        antSourceLinks.add(def)
        return def
    }

    override fun execute() {
        if (sourcePath.list().size() == 0) {
            throw BuildException("At least one source path needs to be specified")
        }
        if (moduleName == null) {
            throw BuildException("Module name needs to be specified")
        }
        if (outputDir == null) {
            throw BuildException("Output directory needs to be specified")
        }
        val sourceLinks = antSourceLinks.map {
            val path = it.path
            if (path == null) {
                throw BuildException("Path attribute of a <sourceLink> element is required")
            }
            val url = it.url
            if (url == null) {
                throw BuildException("Path attribute of a <sourceLink> element is required")
            }
            SourceLinkDefinition(File(path).getCanonicalFile().getAbsolutePath(), url, it.lineSuffix)
        }

        val url = javaClass<DokkaAntTask>().getResource("/org/jetbrains/dokka/ant/DokkaAntTask.class")
        val jarRoot = url.getPath().substringBefore("!/").removePrefix("file:")

        val generator = DokkaGenerator(
                AntLogger(this),
                listOf(jarRoot) + compileClasspath.list().toList(),
                sourcePath.list().toList(),
                samplesPath.list().toList(),
                includesPath.list().toList(),
                moduleName!!,
                outputDir!!,
                outputFormat,
                sourceLinks,
                skipDeprecated
        )
        generator.generate()
    }
}