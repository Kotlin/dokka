package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.InputStream
import java.util.*

/*
* Those are extension names, which are used in a build.gradle file as closure names:
* dokka {
*     configuration { <- extension name
*     }
* }
* */
internal const val CONFIGURATION_EXTENSION_NAME = "configuration"
internal const val MULTIPLATFORM_EXTENSION_NAME = "multiplatform"

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka.properties"))

        project.tasks.create("dokka", DokkaTask::class.java).apply {
            dokkaRuntime = project.configurations.create("dokkaRuntime")
            moduleName = project.name
            outputDirectory = File(project.buildDir, "dokka").absolutePath
        }
        project.tasks.withType(DokkaTask::class.java) { task ->
            val passConfiguration = project.container(GradlePassConfigurationImpl::class.java)
            task.extensions.add(MULTIPLATFORM_EXTENSION_NAME, passConfiguration)
            task.extensions.create(CONFIGURATION_EXTENSION_NAME, GradlePassConfigurationImpl::class.java, "")
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