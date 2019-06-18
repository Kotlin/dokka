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
        val taskName = "dokka"

        defaultDokkaRuntimeConfiguration.defaultDependencies{ dependencies -> dependencies.add(project.dependencies.create("org.jetbrains.dokka:dokka-fatjar:${DokkaVersion.version}")) }

        if(GradleVersion.current() >= GradleVersion.version("4.10")) {
            project.tasks.register(taskName, DokkaTask::class.java).configure {
                it.moduleName = project.name
                it.outputDirectory = File(project.buildDir, taskName).absolutePath
            }
        } else {
            project.tasks.create(taskName, DokkaTask::class.java).apply {
                moduleName = project.name
                outputDirectory = File(project.buildDir, taskName).absolutePath
            }
        }

        project.tasks.withType(DokkaTask::class.java) { task ->
            task.multiplatform = project.container(GradlePassConfigurationImpl::class.java)
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