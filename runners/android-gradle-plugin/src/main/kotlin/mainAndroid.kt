package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class DokkaAndroidPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        DokkaVersion.loadFrom(javaClass.getResourceAsStream("/META-INF/gradle-plugins/org.jetbrains.dokka-android.properties"))
        project.tasks.create("dokka", DokkaAndroidTask::class.java).apply {
            moduleName = project.name
            outputDirectory = File(project.buildDir, "dokka").absolutePath
        }
    }
}

open class DokkaAndroidTask : DokkaTask()
