@file:Suppress("UnstableApiUsage")

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
            task.outputDirectory by task.project.buildDir.resolve("output")
        }

        val multimoduleTasks = rootProject.tasks.withType<DokkaMultiModuleTask>()
        assertTrue(multimoduleTasks.isNotEmpty(), "Expected at least one multimodule task")

        multimoduleTasks.configureEach { task ->
            task.moduleName by "custom Module Name"
            task.documentationFileName by "customDocumentationFileName.md"
            task.outputDirectory by task.project.buildDir.resolve("customOutputDirectory")
            task.cacheRoot by File("customCacheRoot")
            task.pluginsConfiguration.put("pluginA", "configA")
            task.failOnWarning by true
            task.offlineMode by true
        }

        multimoduleTasks.forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertEquals(
                DokkaConfigurationImpl(
                    moduleName = "custom Module Name",
                    outputDir = task.project.buildDir.resolve("customOutputDirectory"),
                    cacheRoot = File("customCacheRoot"),
                    pluginsConfiguration = mapOf("pluginA" to "configA"),
                    pluginsClasspath = emptyList(),
                    failOnWarning = true,
                    offlineMode = true,
                    modules = listOf(
                        DokkaModuleDescriptionImpl(
                            name = "child",
                            path = File("child"),
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
        val multimoduleTasks = rootProject.tasks.withType<DokkaMultiModuleTask>()
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
            task.addSubprojectChildTasks("customDokkaTask")
            val dependencies = task.taskDependencies.getDependencies(task).toSet()

            assertEquals(2, dependencies.size, "Expected two dependencies")
            assertTrue(customDokkaTask in dependencies, "Expected 'customDokkaTask' in dependencies")
        }
    }

    @Test
    fun `multimodule task with no child tasks throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val multimodule = project.tasks.create<DokkaMultiModuleTask>("multimodule")
        project.configurations.configureEach { it.withDependencies { it.clear() } }
        assertFailsWith<DokkaException> { multimodule.generateDocumentation() }
    }

    @Test
    fun childDocumentationFiles() {
        val parent = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()

        val parentTask = parent.tasks.create<DokkaMultiModuleTask>("parent")
        val childTask = child.tasks.create<DokkaTask>("child")

        parentTask.addChildTask(childTask)
        parentTask.documentationFileName by "module.txt"

        assertEquals(
            listOf(parent.file("child/module.txt")), parentTask.childDocumentationFiles,
            "Expected child documentation file being present"
        )
    }

    @Test
    fun sourceChildOutputDirectories() {
        val parent = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()

        val parentTask = parent.tasks.create<DokkaMultiModuleTask>("parent")
        val childTask = child.tasks.create<DokkaTask>("child")

        parentTask.addChildTask(childTask)
        childTask.outputDirectory by child.file("custom/output")

        assertEquals(
            listOf(parent.file("child/custom/output")), parentTask.sourceChildOutputDirectories,
            "Expected child output directory being present"
        )
    }

    @Test
    fun targetChildOutputDirectories() {
        val parent = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()

        val parentTask = parent.tasks.create<DokkaMultiModuleTask>("parent")
        val childTask = child.tasks.create<DokkaTask>("child")

        parentTask.addChildTask(childTask)
        parentTask.fileLayout by object : DokkaMultiModuleFileLayout {
            override fun targetChildOutputDirectory(parent: DokkaMultiModuleTask, child: AbstractDokkaTask): File {
                return parent.project.buildDir.resolve(child.name)
            }
        }

        assertEquals(
            listOf(parent.project.buildDir.resolve("child")), parentTask.targetChildOutputDirectories,
            "Expected child target output directory being present"
        )

    }
}
