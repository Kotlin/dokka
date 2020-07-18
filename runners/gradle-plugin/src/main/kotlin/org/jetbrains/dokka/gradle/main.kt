package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.setupDokkaTasks("dokkaHtml")

        project.setupDokkaTasks(
            name = "dokkaJavadoc",
            multimoduleTaskSupported = false
        ) {
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
     * Creates [DokkaTask], [DokkaMultimoduleTask] for the given
     * name and configuration.
     */
    private fun Project.setupDokkaTasks(
        name: String,
        multimoduleTaskSupported: Boolean = true,
        collectorTaskSupported: Boolean = true,
        configuration: AbstractDokkaTask.() -> Unit = {}
    ) {
        project.maybeCreateDokkaPluginConfiguration(name)
        project.maybeCreateDokkaRuntimeConfiguration(name)
        project.tasks.register<DokkaTask>(name) {
            configuration()
        }

        if (project.subprojects.isNotEmpty()) {
            if (multimoduleTaskSupported) {
                val multimoduleName = "${name}Multimodule"
                project.maybeCreateDokkaPluginConfiguration(multimoduleName)
                project.maybeCreateDokkaRuntimeConfiguration(multimoduleName)
                project.tasks.register<DokkaMultimoduleTask>(multimoduleName) {
                    dokkaTaskNames = dokkaTaskNames + name
                    configuration()
                }
            }
            if (collectorTaskSupported) {
                project.tasks.register<DokkaCollectorTask>("${name}Collector") {
                    dokkaTaskNames = dokkaTaskNames + name
                }
            }
        }
    }
}
