/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.gradle.DOKKA_V1_DEPRECATION_MESSAGE

/**
 * The OG Dokka Gradle Plugin. A.K.A. DGP Classic, or Dokka V1.
 *
 * This plugin is planned for removal, see https://youtrack.jetbrains.com/issue/KT-71027/Remove-Dokka-Gradle-Plugin-v1
 */
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
open class DokkaClassicPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (GradleVersion.version(project.gradle.gradleVersion) < GradleVersion.version("5.6")) {
            project.logger.warn("Dokka: Build is using unsupported gradle version, expected at least 5.6 but got ${project.gradle.gradleVersion}. This may result in strange errors")
        }

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

        project.setupDokkaTasks(
            "dokkaGfm",
            allModulesPageAndTemplateProcessing = project.dokkaArtifacts.gfmTemplateProcessing
        ) {
            plugins.dependencies.add(project.dokkaArtifacts.gfmPlugin)
            description = "Generates documentation in GitHub flavored markdown format"
        }

        project.setupDokkaTasks(
            "dokkaJekyll",
            allModulesPageAndTemplateProcessing = project.dokkaArtifacts.jekyllTemplateProcessing
        ) {
            plugins.dependencies.add(project.dokkaArtifacts.jekyllPlugin)
            description = "Generates documentation in Jekyll flavored markdown format"
        }

        project.configureEachAbstractDokkaTask()
        project.configureEachDokkaMultiModuleTask()
    }

    /**
     * Creates [DokkaTask], [DokkaMultiModuleTask] for the given
     * name and configuration.
     */
    private fun Project.setupDokkaTasks(
        name: String,
        multiModuleTaskSupported: Boolean = true,
        allModulesPageAndTemplateProcessing: Dependency = project.dokkaArtifacts.allModulesPage,
        configuration: @Suppress("DEPRECATION") AbstractDokkaTask.() -> Unit = {}
    ) {
        project.maybeCreateDokkaPluginConfiguration(name)
        project.maybeCreateDokkaRuntimeConfiguration(name)
        project.tasks.register<@Suppress("DEPRECATION") DokkaTask>(name) {
            configuration()
        }

        if (project.parent != null) {
            val partialName = "${name}Partial"
            project.maybeCreateDokkaPluginConfiguration(partialName)
            project.maybeCreateDokkaRuntimeConfiguration(partialName)
            project.tasks.register<@Suppress("DEPRECATION") DokkaTaskPartial>(partialName) {
                configuration()
            }
        }

        if (project.subprojects.isNotEmpty()) {
            if (multiModuleTaskSupported) {
                val multiModuleName = "${name}MultiModule"
                project.maybeCreateDokkaPluginConfiguration(multiModuleName, setOf(allModulesPageAndTemplateProcessing))
                project.maybeCreateDokkaRuntimeConfiguration(multiModuleName)

                project.tasks.register<@Suppress("DEPRECATION") DokkaMultiModuleTask>(multiModuleName) {
                    @Suppress("DEPRECATION")
                    addSubprojectChildTasks("${name}Partial")
                    configuration()
                    description = "Runs all subprojects '$name' tasks and generates module navigation page"
                }

                project.tasks.register<DefaultTask>("${name}Multimodule") {
                    group = "deprecated"
                    description = "DEPRECATED: 'Multimodule' is deprecated. Use 'MultiModule' instead."
                    dependsOn(multiModuleName)
                    doLast {
                        logger.warn("'Multimodule' is deprecated. Use 'MultiModule' instead")
                    }
                }
            }

            project.tasks.register<@Suppress("DEPRECATION") DokkaCollectorTask>("${name}Collector") {
                @Suppress("DEPRECATION")
                addSubprojectChildTasks(name)
                description =
                    "Generates documentation merging all subprojects '$name' tasks into one virtual module"
            }
        }
    }

    private fun Project.configureEachAbstractDokkaTask() {
        tasks.withType<@Suppress("DEPRECATION") AbstractDokkaTask>().configureEach task@{
            val formatClassifier = name.removePrefix("dokka").decapitalize()
            outputDirectory.convention(project.layout.buildDirectory.dir("dokka/$formatClassifier"))
            cacheRoot.convention(project.layout.dir(providers.provider { DokkaDefaults.cacheRoot }))

            pluginsClasspath.from(this@task.plugins)
            runtimeClasspath.from(this@task.runtime)
        }
    }

    private fun Project.configureEachDokkaMultiModuleTask() {
        tasks.withType<@Suppress("DEPRECATION") DokkaMultiModuleTask>().configureEach {
            sourceChildOutputDirectories.from({ childDokkaTasks.map { it.outputDirectory } })
        }
    }
}
