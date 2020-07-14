package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.setupDokkaTasks("dokkaHtml")

        project.setupDokkaTasks("dokkaJavadoc") {
            plugins.dependencies.add(project.dokkaArtifacts.javadocPlugin)
        }

        project.setupDokkaTasks("dokkaGfm") {
            plugins.dependencies.add(project.dokkaArtifacts.gfmPlugin)
        }

        project.setupDokkaTasks("dokkaJekyll") {
            plugins.dependencies.add(project.dokkaArtifacts.jekyllPlugin)
        }
    }

    /**
     * Creates [DokkaTask], [DokkaMultimoduleTask] and [DokkaCollectorTask] for the given
     * name and configuration.
     */
    private fun Project.setupDokkaTasks(name: String, configuration: AbstractDokkaTask.() -> Unit = {}) {
        project.maybeCreateDokkaPluginConfiguration(name)
        project.maybeCreateDokkaRuntimeConfiguration(name)
        project.tasks.register<DokkaTask>(name) {
            configuration()
        }

        val multimoduleName = "${name}Multimodule"
        project.maybeCreateDokkaPluginConfiguration(multimoduleName)
        project.maybeCreateDokkaRuntimeConfiguration(multimoduleName)
        project.tasks.register<DokkaMultimoduleTask>(multimoduleName) {
            dokkaTaskNames = dokkaTaskNames + name
            configuration()
        }

        project.tasks.register<DokkaCollectorTask>("${name}Collector") {
            dokkaTaskNames = dokkaTaskNames + name
        }
    }
}
