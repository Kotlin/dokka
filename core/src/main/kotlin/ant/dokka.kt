package org.jetbrains.dokka.ant

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Project
import org.apache.tools.ant.Task
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.Reference
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.SourceLinkDefinition
import java.io.File

class AntLogger(val task: Task): DokkaLogger {
    override fun info(message: String) = task.log(message, Project.MSG_INFO)
    override fun warn(message: String) = task.log(message, Project.MSG_WARN)
    override fun error(message: String) = task.log(message, Project.MSG_ERR)
}

class AntSourceLinkDefinition(var path: String? = null, var url: String? = null, var lineSuffix: String? = null)

class DokkaAntTask(): Task() {
    var moduleName: String? = null
    var outputDir: String? = null
    var outputFormat: String = "html"

    var skipDeprecated: Boolean = false

    val compileClasspath: Path = Path(getProject())
    val sourcePath: Path = Path(getProject())
    val samplesPath: Path = Path(getProject())
    val includesPath: Path = Path(getProject())

    val antSourceLinks: MutableList<AntSourceLinkDefinition> = arrayListOf()

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

    override fun execute() {
        if (sourcePath.list().size == 0) {
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
            SourceLinkDefinition(File(path).canonicalFile.absolutePath, url, it.lineSuffix)
        }

        val url = DokkaAntTask::class.java.getResource("/org/jetbrains/dokka/ant/DokkaAntTask.class")
        val jarRoot = url.path.substringBefore("!/").removePrefix("file:")

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