package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DokkaMultiModuleTaskTest {

    private val rootProject = ProjectBuilder.builder()
        .withName("root")
        .build()

    private val childProject = ProjectBuilder.builder()
        .withName("child")
        .withProjectDir(rootProject.projectDir.resolve("child"))
        .withParent(rootProject).build()

    init {
        rootProject.allprojects { project ->
            project.plugins.apply("org.jetbrains.kotlin.jvm")
            project.plugins.apply("org.jetbrains.dokka")
            project.tasks.withType<AbstractDokkaTask>().configureEach { task ->
                task.plugins.withDependencies { dependencies -> dependencies.clear() }
            }
        }
    }

    @Test
    fun `child project is withing root project`() {
        assertEquals(
            rootProject.projectDir, childProject.projectDir.parentFile,
            "Expected child project being inside the root project"
        )

        assertEquals(
            childProject.projectDir.name, "child",
            "Expected folder of child project to be called 'child'"
        )
    }

    @Test
    fun buildDokkaConfiguration() {
        childProject.tasks.withType<DokkaTask>().configureEach { task ->
            task.outputDirectory = task.project.buildDir.resolve("output")
        }

        val multimoduleTasks = rootProject.tasks.withType<DokkaMultimoduleTask>()
        assertTrue(multimoduleTasks.isNotEmpty(), "Expected at least one multimodule task")

        multimoduleTasks.configureEach { task ->
            task.documentationFileName = "customDocumentationFileName.md"
            task.outputDirectory = task.project.buildDir.resolve("customOutputDirectory")
            task.cacheRoot = File("customCacheRoot")
            task.pluginsConfiguration["pluginA"] = "configA"
            task.failOnWarning = true
            task.offlineMode = true
        }

        multimoduleTasks.forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertEquals(
                DokkaConfigurationImpl(
                    outputDir = task.project.buildDir.resolve("customOutputDirectory"),
                    cacheRoot = File("customCacheRoot"),
                    pluginsConfiguration = mapOf("pluginA" to "configA"),
                    pluginsClasspath = emptyList(),
                    failOnWarning = true,
                    offlineMode = true,
                    modules = listOf(
                        DokkaModuleDescriptionImpl(
                            name = "child",
                            path = File("../../child/build/output"),
                            docFile = childProject.projectDir.resolve("customDocumentationFileName.md")
                        )
                    )
                ),
                dokkaConfiguration
            )
        }
    }

    @Test
    fun `setting dokkaTaskNames declares proper task dependencies`() {
        val multimoduleTasks = rootProject.tasks.withType<DokkaMultimoduleTask>()
        assertTrue(multimoduleTasks.isNotEmpty(), "Expected at least one multimodule task")

        multimoduleTasks.toList().forEach { task ->
            val dependencies = task.taskDependencies.getDependencies(task).toSet()
            assertEquals(1, dependencies.size, "Expected one dependency")
            val dependency = dependencies.single()

            assertTrue(dependency is DokkaTask, "Expected dependency to be of Type ${DokkaTask::class.simpleName}")
            assertEquals(childProject, dependency.project, "Expected dependency from child project")
        }

        val customDokkaTask = childProject.tasks.create<DokkaTask>("customDokkaTask")

        multimoduleTasks.toList().forEach { task ->
            task.dokkaTaskNames += "customDokkaTask"
            val dependencies = task.taskDependencies.getDependencies(task).toSet()

            assertEquals(2, dependencies.size, "Expected two dependencies")
            assertTrue(customDokkaTask in dependencies, "Expected 'customDokkaTask' in dependencies")
        }
    }
}
