/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage", "DEPRECATION", "PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.gradle.utils.allprojects_
import org.jetbrains.dokka.gradle.utils.configureEach_
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.withDependencies_
import java.io.File
import kotlin.test.*

class DokkaMultiModuleTaskTest {

    private val rootProject = ProjectBuilder.builder()
        .withName("root")
        .build()

    private val childProject = ProjectBuilder.builder()
        .withName("child")
        .withProjectDir(rootProject.projectDir.resolve("child"))
        .withParent(rootProject).build()

    private val childDokkaTask = childProject.tasks.create<DokkaTaskPartial>("childDokkaTask")

    private val multiModuleTask = rootProject.tasks.create<DokkaMultiModuleTask>("multiModuleTask").apply {
        addChildTask(childDokkaTask)
    }

    init {
        rootProject.plugins.apply("org.jetbrains.dokka")
        childProject.plugins.apply("org.jetbrains.dokka")
        rootProject.allprojects_ {
            tasks.withType<AbstractDokkaTask>().configureEach_ {
                plugins.withDependencies_ { clear() }
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
        val topLevelInclude = multiModuleTask.project.file("README.md")

        childDokkaTask.apply {
            dokkaSourceSets.create("main")
            dokkaSourceSets.create("test")
            dokkaSourceSets.configureEach_ {
                includes.from(include1, include2)
            }
        }

        multiModuleTask.apply {
            moduleVersion.set("1.5.0")
            moduleName.set("custom Module Name")
            outputDirectory.set(project.layout.buildDirectory.dir("customOutputDirectory"))
            cacheRoot.set(File("customCacheRoot"))
            pluginsConfiguration.add(
                PluginConfigurationImpl(
                    "pluginA",
                    DokkaConfiguration.SerializationFormat.JSON,
                    """ { "key" : "value2" } """
                )
            )
            failOnWarning.set(true)
            offlineMode.set(true)
            includes.from(listOf(topLevelInclude))
        }

        val dokkaConfiguration = multiModuleTask.buildDokkaConfiguration()
        assertEquals(
            DokkaConfigurationImpl(
                moduleName = "custom Module Name",
                moduleVersion = "1.5.0",
                outputDir = multiModuleTask.project.layout.buildDirectory.dir("customOutputDirectory").get().asFile,
                cacheRoot = multiModuleTask.project.layout.projectDirectory.dir("customCacheRoot").asFile,
                pluginsConfiguration = mutableListOf(
                    PluginConfigurationImpl(
                        "pluginA",
                        DokkaConfiguration.SerializationFormat.JSON,
                        """ { "key" : "value2" } """
                    )
                ),
                pluginsClasspath = emptyList(),
                failOnWarning = true,
                offlineMode = true,
                includes = setOf(topLevelInclude),
                modules = listOf(
                    DokkaModuleDescriptionImpl(
                        name = "child",
                        relativePathToOutputDirectory = File("child"),
                        includes = setOf(include1, include2),
                        sourceOutputDirectory = childDokkaTask.outputDirectory.get().asFile
                    )
                )
            ),
            dokkaConfiguration
        )
    }

    @Test
    fun `multimodule task should not include unspecified version`() {
        childDokkaTask.apply {
            dokkaSourceSets.create("main")
            dokkaSourceSets.create("test")
        }

        multiModuleTask.apply {
            moduleVersion.set("unspecified")
        }

        val dokkaConfiguration = multiModuleTask.buildDokkaConfiguration()
        assertNull(dokkaConfiguration.moduleVersion)
    }

    @Test
    fun `setting dokkaTaskNames declares proper task dependencies`() {
        val dependenciesInitial = multiModuleTask.taskDependencies.getDependencies(multiModuleTask).toSet()
        assertEquals(1, dependenciesInitial.size, "Expected one dependency")
        val dependency = dependenciesInitial.single()

        assertTrue(
            dependency is DokkaTaskPartial,
            "Expected dependency to be of Type ${DokkaTaskPartial::class.simpleName}"
        )
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
        project.configurations.configureEach_ { withDependencies_ { clear() } }
        assertFailsWith<DokkaException> { multimodule.generateDocumentation() }
    }

    @Test
    fun childDokkaTaskIncludes() {
        val childDokkaTaskInclude1 = childProject.file("include1")
        val childDokkaTaskInclude2 = childProject.file("include2")
        val childDokkaTaskInclude3 = childProject.file("include3")

        childDokkaTask.apply {
            dokkaSourceSets.create_("main") {
                includes.from(childDokkaTaskInclude1, childDokkaTaskInclude2)
            }
            dokkaSourceSets.create_("main2") {
                includes.from(childDokkaTaskInclude3)
            }
        }

        val secondChildDokkaTaskInclude = childProject.file("include4")
        val secondChildDokkaTask = childProject.tasks.create<DokkaTaskPartial>("secondChildDokkaTask") {
            dokkaSourceSets.create_("main") {
                includes.from(secondChildDokkaTaskInclude)
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
        parent.plugins.apply("org.jetbrains.dokka")
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()
        child.plugins.apply("org.jetbrains.dokka")

        val parentTask = parent.tasks.create<DokkaMultiModuleTask>("parent")
        val childTask = child.tasks.create<DokkaTask>("child")

        parentTask.addChildTask(childTask)
        childTask.outputDirectory.set(child.file("custom/output"))

        assertEquals(
            listOf(parent.file("child/custom/output")),
            parentTask.sourceChildOutputDirectories.files.toList(),
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

        parentTask.fileLayout.set(DokkaMultiModuleFileLayout { taskParent, taskChild ->
            taskParent.project.layout.buildDirectory.dir(taskChild.name)
        })

        assertEquals(
            listOf(parent.project.layout.buildDirectory.dir("child").get().asFile),
            parentTask.targetChildOutputDirectories.get().map { it.asFile },
            "Expected child target output directory being present"
        )
    }
}
