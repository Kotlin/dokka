package org.jetbrains.dokka.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.SourceLinkDefinition
import org.jetbrains.dokka.DocumentationOptions

class SourceLinkMapItem {
    @Parameter(name = "dir", required = true)
    var dir: String = ""

    @Parameter(name = "url", required = true)
    var url: String = ""

    @Parameter(name = "urlSuffix")
    var urlSuffix: String? = null
}

@Mojo(name = "dokka", defaultPhase = LifecyclePhase.PRE_SITE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
class DokkaMojo : AbstractMojo() {
    @Parameter(required = true, defaultValue = "\${project.compileSourceRoots}")
    var sourceDirectories: List<String> = emptyList()

    @Parameter
    var samplesDirs: List<String> = emptyList()

    @Parameter
    var includeDirs: List<String> = emptyList()

    @Parameter(required = true, defaultValue = "\${project.compileClasspathElements}")
    var classpath: List<String> = emptyList()

    @Parameter(required = true, defaultValue = "\${project.basedir}/target/dokka")
    var outputDir: String = ""

    @Parameter(required = true, defaultValue = "html")
    var outputFormat: String = "html"

    @Parameter
    var sourceLinks: Array<SourceLinkMapItem> = emptyArray()

    @Parameter(required = true, defaultValue = "\${project.artifactId}")
    var moduleName: String = ""

    @Parameter(required = false, defaultValue = "false")
    var skip: Boolean = false

    @Parameter(required = false, defaultValue = "6")
    var jdkVersion: Int = 6

    override fun execute() {
        if (skip) {
            log.info("Dokka skip parameter is true so no dokka output will be produced")
            return
        }

        val gen = DokkaGenerator(
                MavenDokkaLogger(log),
                classpath,
                sourceDirectories,
                samplesDirs,
                includeDirs,
                moduleName,
                DocumentationOptions(outputDir, outputFormat,
                        sourceLinks = sourceLinks.map { SourceLinkDefinition(it.dir, it.url, it.urlSuffix) },
                        jdkVersion = jdkVersion
                )
        )

        gen.generate()
    }
}
