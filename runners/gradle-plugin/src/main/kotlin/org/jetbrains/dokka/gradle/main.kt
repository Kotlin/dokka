package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

open class DokkaPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.setupDokkaTasks("dokkaHtml") {
            description = "Generates documentation in 'html' format"
        }

        project.setupDokkaTasks(
            name = "dokkaJavadoc",
            multiModuleTaskSupported = false
        ) {
            plugins.dependencies.add(project.dokkaArtifacts.javadocPlugin)
            description = "Generates documentation in 'javadoc' format"
        }

        project.setupDokkaTasks("dokkaGfm") {
            plugins.dependencies.add(project.dokkaArtifacts.gfmPlugin)
            description = "Generates documentation in GitHub flavored markdown format"
        }

        project.setupDokkaTasks("dokkaJekyll") {
            plugins.dependencies.add(project.dokkaArtifacts.jekyllPlugin)
            description = "Generates documentation in Jekyll flavored markdown format"
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
                    description = "Runs all subprojects '$name' tasks and generates module navigation page"
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
                    description =
                        "Generates documentation merging all subprojects '$name' tasks into one virtual module"
                }
            }
        }
    }
}
