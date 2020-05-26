package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.GradleVersion
import java.io.File
import java.io.InputStream
import java.util.*

internal const val CONFIGURATION_EXTENSION_NAME = "configuration"
internal const val SOURCE_SETS_EXTENSION_NAME = "dokkaSourceSets"
internal const val DOKKA_TASK_NAME = "dokka"
internal const val DOKKA_COLLECTOR_TASK_NAME = "dokkaCollector"
internal const val DOKKA_MULTIMODULE_TASK_NAME = "dokkaMultimodule"

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        loadDokkaVersion()
        val dokkaRuntimeConfiguration = addConfiguration(project)
        val pluginsConfiguration = project.configurations.create("dokkaPlugins").apply {
            dependencies.add(project.dependencies.create("org.jetbrains.dokka:dokka-base:${DokkaVersion.version}"))
        }
        addDokkaTasks(project, dokkaRuntimeConfiguration, pluginsConfiguration, DokkaTask::class.java)
        addDokkaCollectorTasks(project, DokkaCollectorTask::class.java)
        addDokkaMultimoduleTasks(project.rootProject, dokkaRuntimeConfiguration, pluginsConfiguration, DokkaMultimoduleTask::class.java)
    }

    private fun loadDokkaVersion() =
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))

    private fun addConfiguration(project: Project) =
        project.configurations.create("dokkaRuntime").apply {
            defaultDependencies { dependencies -> dependencies.add(project.dependencies.create("org.jetbrains.dokka:dokka-core:${DokkaVersion.version}")) }
        }

    private fun addDokkaTasks(
        project: Project,
        runtimeConfiguration: Configuration,
        pluginsConfiguration: Configuration,
        taskClass: Class<out DokkaTask>
    ) {
        if (GradleVersion.current() >= GradleVersion.version("4.10")) {
            project.tasks.register(DOKKA_TASK_NAME, taskClass)
        } else {
            project.tasks.create(DOKKA_TASK_NAME, taskClass)
        }
        project.tasks.withType(taskClass) { task ->
            task.dokkaSourceSets = project.container(GradlePassConfigurationImpl::class.java)
            task.configuration = GradlePassConfigurationImpl()
            task.dokkaRuntime = runtimeConfiguration
            task.pluginsConfig = pluginsConfiguration
            task.outputDirectory = File(project.rootProject.buildDir, "$DOKKA_TASK_NAME/${project.name}").absolutePath
        }
    }

    private fun addDokkaCollectorTasks(
        project: Project,
        taskClass: Class<out DokkaCollectorTask>
    ) {
        if (GradleVersion.current() >= GradleVersion.version("4.10")) {
            project.tasks.register(DOKKA_COLLECTOR_TASK_NAME, taskClass)
        } else {
            project.tasks.create(DOKKA_COLLECTOR_TASK_NAME, taskClass)
        }
        project.tasks.withType(taskClass) { task ->
            task.modules = emptyList()
            task.outputDirectory = File(project.buildDir, DOKKA_TASK_NAME).absolutePath
        }
    }

    private fun addDokkaMultimoduleTasks(
        project: Project,
        runtimeConfiguration: Configuration,
        pluginsConfiguration: Configuration,
        taskClass: Class<out DokkaMultimoduleTask>
    ) {
        if (project.tasks.find { it.name == DOKKA_MULTIMODULE_TASK_NAME } == null) {
            if (GradleVersion.current() >= GradleVersion.version("4.10")) {
                project.tasks.register(DOKKA_MULTIMODULE_TASK_NAME, taskClass)
            } else {
                project.tasks.create(DOKKA_MULTIMODULE_TASK_NAME, taskClass)
            }
            project.tasks.withType(taskClass) { task ->
                task.dokkaRuntime = runtimeConfiguration
                task.pluginsConfig = pluginsConfiguration
                task.outputDirectory = File(project.buildDir, DOKKA_TASK_NAME).absolutePath
            }
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