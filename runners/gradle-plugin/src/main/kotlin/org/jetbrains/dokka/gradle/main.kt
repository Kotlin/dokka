package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import java.io.File

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.createDokkaTasks("dokka")

        project.createDokkaTasks("dokkaJavadoc") {
            plugins.dependencies.add(project.dokkaArtifacts.javadocPlugin)
        }

        project.createDokkaTasks("dokkaGfm") {
            plugins.dependencies.add(project.dokkaArtifacts.gfmPlugin)
        }

        project.createDokkaTasks("dokkaJekyll") {
            plugins.dependencies.add(project.dokkaArtifacts.jekyllPlugin)
        }
    }

    /**
     * Creates [DokkaTask], [DokkaMultimoduleTask] and [DokkaCollectorTask] for the given
     * name and configuration.
     *
     * The tasks are created, not registered to enable gradle's accessor generation like
     * ```
     * dependencies {
     *     dokkaPlugin(":my-group:my-plugin:my-version)
     * }
     * ```
     *
     * There is no heavy processing done during configuration of those tasks (I promise).
     */
    private fun Project.createDokkaTasks(name: String, configuration: DokkaTask.() -> Unit = {}) {
        project.tasks.create<DokkaTask>(name) {
            outputDirectory = File(buildDir, name).absolutePath
            configuration()
        }

        project.tasks.create<DokkaCollectorTask>("${name}Collector") {
            outputDirectory = File(buildDir, name).absolutePath
            dokkaTaskNames.add(name)
        }

        project.tasks.create<DokkaMultimoduleTask>("${name}Multimodule") {
            outputDirectory = File(buildDir, name).absolutePath
            dokkaTaskNames.add(name)
        }
    }
}
