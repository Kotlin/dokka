@file:Suppress("UnstableApiUsage")

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.*
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

    private val childDokkaTask = childProject.tasks.create<DokkaTask>("childDokkaTask")

    private val multiModuleTask = rootProject.tasks.create<DokkaMultiModuleTask>("multiModuleTask").apply {
        addChildTask(childDokkaTask)
    }

    init {
        rootProject.allprojects { project ->
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
        val include1 = childDokkaTask.project.file("include1.md")
        val include2 = childDokkaTask.project.file("include2.md")

        childDokkaTask.apply {
            dokkaSourceSets.create("main")
            dokkaSourceSets.create("test")
            dokkaSourceSets.configureEach {
                it.includes.from(include1, include2)
            }
        }

        multiModuleTask.apply {
            moduleName by "custom Module Name"
            outputDirectory by project.buildDir.resolve("customOutputDirectory")
            cacheRoot by File("customCacheRoot")
            pluginsConfiguration.put("pluginA", "configA")
            failOnWarning by true
            offlineMode by true
        }

        val dokkaConfiguration = multiModuleTask.buildDokkaConfiguration()
        assertEquals(
            DokkaConfigurationImpl(
                moduleName = "custom Module Name",
                outputDir = multiModuleTask.project.buildDir.resolve("customOutputDirectory"),
                cacheRoot = File("customCacheRoot"),
                pluginsConfiguration = mapOf("pluginA" to "configA"),
                pluginsClasspath = emptyList(),
                failOnWarning = true,
                offlineMode = true,
                modules = listOf(
                    DokkaModuleDescriptionImpl(
                        name = "child",
                        relativePathToOutputDirectory = File("child"),
                        includes = setOf(include1, include2)
                    )
                )
            ),
            dokkaConfiguration
        )
    }

    @Test
    fun `setting dokkaTaskNames declares proper task dependencies`() {
        val dependenciesInitial = multiModuleTask.taskDependencies.getDependencies(multiModuleTask).toSet()
        assertEquals(1, dependenciesInitial.size, "Expected one dependency")
        val dependency = dependenciesInitial.single()

        assertTrue(dependency is DokkaTask, "Expected dependency to be of Type ${DokkaTask::class.simpleName}")
        assertEquals(childProject, dependency.project, "Expected dependency from child project")


        val customDokkaTask = childProject.tasks.create<DokkaTask>("customDokkaTask")

        multiModuleTask.addSubprojectChildTasks("customDokkaTask")
        val dependenciesAfter = multiModuleTask.taskDependencies.getDependencies(multiModuleTask).toSet()

        assertEquals(2, dependenciesAfter.size, "Expected two dependencies")
        assertTrue(customDokkaTask in dependenciesAfter, "Expected 'customDokkaTask' in dependencies")

    }

    @Test
    fun `multimodule task with no child tasks throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val multimodule = project.tasks.create<DokkaMultiModuleTask>("multimodule")
        project.configurations.configureEach { it.withDependencies { it.clear() } }
        assertFailsWith<DokkaException> { multimodule.generateDocumentation() }
    }

    @Test
    fun childDokkaTaskIncludes() {
        val childDokkaTaskInclude1 = childProject.file("include1")
        val childDokkaTaskInclude2 = childProject.file("include2")
        val childDokkaTaskInclude3 = childProject.file("include3")

        childDokkaTask.apply {
            dokkaSourceSets.create("main") {
                it.includes.from(childDokkaTaskInclude1, childDokkaTaskInclude2)
            }
            dokkaSourceSets.create("main2") {
                it.includes.from(childDokkaTaskInclude3)
            }
        }

        val secondChildDokkaTaskInclude = childProject.file("include4")
        val secondChildDokkaTask = childProject.tasks.create<DokkaTask>("secondChildDokkaTask") {
            dokkaSourceSets.create("main") {
                it.includes.from(secondChildDokkaTaskInclude)
            }
        }
        multiModuleTask.addChildTask(secondChildDokkaTask)

        assertEquals(
            mapOf(
                ":child:childDokkaTask" to setOf(
                    childDokkaTaskInclude1,
                    childDokkaTaskInclude2,
                    childDokkaTaskInclude3
                ),
                ":child:secondChildDokkaTask" to setOf(secondChildDokkaTaskInclude)
            ),
            multiModuleTask.childDokkaTaskIncludes
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
