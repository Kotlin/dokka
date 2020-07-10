package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.createDokkaTasks("dokka") {
            outputDirectory = defaultDokkaOutputDirectory(project.buildDir, "dokkaKdoc").absolutePath
            doFirst {
                logger.warn(":dokka task is deprecated in favor of :dokkaKdoc")
            }
        }

        project.createDokkaTasks("dokkaKdoc")

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
    private fun Project.createDokkaTasks(name: String, configuration: AbstractDokkaTask.() -> Unit = {}) {
        project.tasks.create<DokkaTask>(name) {
            configuration()
        }

        project.tasks.create<DokkaMultimoduleTask>("${name}Multimodule") {
            dokkaTaskNames.add(name)
            configuration()
        }

        project.tasks.create<DokkaCollectorTask>("${name}Collector") {
            dokkaTaskNames.add(name)
        }
    }
}
