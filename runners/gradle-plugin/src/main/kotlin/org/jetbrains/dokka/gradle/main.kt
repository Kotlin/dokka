package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.GradleVersion
import java.io.File
import java.io.InputStream
import java.util.*

internal const val CONFIGURATION_EXTENSION_NAME = "configuration"
internal const val MULTIPLATFORM_EXTENSION_NAME = "multiplatform"

open class DokkaPlugin : Plugin<Project> {
    private val taskName = "dokka"

    override fun apply(project: Project) {
        loadDokkaVersion()
        val dokkaRuntimeConfiguration = addConfiguration(project)
        addTasks(project, dokkaRuntimeConfiguration, DokkaTask::class.java)
    }

    private fun loadDokkaVersion() = DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))

    private fun addConfiguration(project: Project) =
        project.configurations.create("dokkaRuntime").apply {
            defaultDependencies{ dependencies -> dependencies.add(project.dependencies.create("org.jetbrains.dokka:dokka-fatjar:${DokkaVersion.version}")) }
        }

    protected open fun addTasks(project: Project, runtimeConfiguration: Configuration, taskClass: Class<out DokkaTask>) {
        if(GradleVersion.current() >= GradleVersion.version("4.10")) {
            project.tasks.register(taskName, taskClass)
        } else {
            project.tasks.create(taskName, taskClass)
        }
        project.tasks.withType(taskClass) { task ->
            task.multiplatform = project.container(GradlePassConfigurationImpl::class.java)
            task.configuration = GradlePassConfigurationImpl()
            task.dokkaRuntime = runtimeConfiguration
            task.outputDirectory = File(project.buildDir, taskName).absolutePath
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