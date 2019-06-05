package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.io.File
import java.io.InputStream
import java.util.*

internal const val CONFIGURATION_EXTENSION_NAME = "configuration"
internal const val MULTIPLATFORM_EXTENSION_NAME = "multiplatform"

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))

        val dokkaRuntimeConfiguration = project.configurations.create("dokkaRuntime")
        val defaultDokkaRuntimeConfiguration = project.configurations.create("defaultDokkaRuntime")

        defaultDokkaRuntimeConfiguration.defaultDependencies{ dependencies -> dependencies.add(project.dependencies.create("org.jetbrains.dokka:dokka-fatjar:${DokkaVersion.version}")) }

        if(GradleVersion.current() >= GradleVersion.version("4.10")) {
            project.tasks.register("dokka", DokkaTask::class.java).configure {
                it.moduleName = project.name
                it.outputDirectory = File(project.buildDir, "dokka").absolutePath
            }
        } else {
            project.tasks.create("dokka", DokkaTask::class.java).apply {
                moduleName = project.name
                outputDirectory = File(project.buildDir, "dokka").absolutePath
            }
        }

        project.tasks.withType(DokkaTask::class.java) { task ->
            val passConfiguration = project.container(GradlePassConfigurationImpl::class.java)
            task.multiplatform = passConfiguration
            task.configuration = GradlePassConfigurationImpl()
            task.dokkaRuntime = dokkaRuntimeConfiguration
            task.defaultDokkaRuntime = defaultDokkaRuntimeConfiguration
        }
    }
}

object DokkaVersion {
    var version: String? = null

    fun loadFrom(stream: InputStream) {
        version = Properties().apply {
            load(stream)
        }.getProperty("dokka-version")
    }
}

object ClassloaderContainer {
    @JvmField
    var fatJarClassLoader: ClassLoader? = null
}