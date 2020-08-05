package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.setupDokkaTasks("dokkaHtml")

        project.setupDokkaTasks(
            name = "dokkaJavadoc",
            multiModuleTaskSupported = false
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
     * Creates [DokkaTask], [DokkaMultiModuleTask] for the given
     * name and configuration.
     */
    private fun Project.setupDokkaTasks(
        name: String,
        multiModuleTaskSupported: Boolean = true,
        collectorTaskSupported: Boolean = true,
        configuration: AbstractDokkaTask.() -> Unit = {}
    ) {
        project.maybeCreateDokkaPluginConfiguration(name)
        project.maybeCreateDokkaRuntimeConfiguration(name)
        project.tasks.register<DokkaTask>(name) {
            configuration()
        }

        if (project.subprojects.isNotEmpty()) {
            if (multiModuleTaskSupported) {
                val multiModuleName = "${name}MultiModule"
                project.maybeCreateDokkaPluginConfiguration(multiModuleName)
                project.maybeCreateDokkaRuntimeConfiguration(multiModuleName)
                project.tasks.register<DokkaMultiModuleTask>(multiModuleName) {
                    addSubprojectChildTasks(name)
                    configuration()
                }
                project.tasks.register<DefaultTask>("${name}Multimodule") {
                    dependsOn(multiModuleName)
                    doLast {
                        logger.warn("'Multimodule' is deprecated. Use 'MultiModule' instead")
                    }
                }
            }
            if (collectorTaskSupported) {
                project.tasks.register<DokkaCollectorTask>("${name}Collector") {
                    addSubprojectChildTasks(name)
                }
            }
        }
    }
}
