package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink.Builder
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import java.io.File

open class DokkaAndroidPlugin : DokkaPlugin() {

    override fun apply(project: Project) {
        loadDokkaVersion()
        val dokkaRuntimeConfiguration = addConfiguration(project)
        addTasks(project, dokkaRuntimeConfiguration, DokkaAndroidTask::class.java)
    }
}

private val ANDROID_REFERENCE_URL = Builder("https://developer.android.com/reference/").build()

open class DokkaAndroidTask : DokkaTask() {

    @Input var noAndroidSdkLink: Boolean = false

    override fun collectSuppressedFiles(sourceRoots: List<SourceRoot>): List<String> {
        val generatedRoot = project.buildDir.resolve("generated").absoluteFile
        return sourceRoots
            .map { File(it.path) }
            .filter { it.startsWith(generatedRoot) }
            .flatMap { it.walk().toList() }
            .map { it.absolutePath }
    }

    init {
        project.afterEvaluate {
            if (!noAndroidSdkLink) externalDocumentationLinks.add(ANDROID_REFERENCE_URL)
        }
    }
}
