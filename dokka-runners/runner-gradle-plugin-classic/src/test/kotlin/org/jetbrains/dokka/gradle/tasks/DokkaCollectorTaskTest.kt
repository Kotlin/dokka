/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.tasks

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaCollectorTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.utils.all_
import org.jetbrains.dokka.gradle.utils.allprojects_
import org.jetbrains.dokka.gradle.utils.configureEach_
import org.jetbrains.dokka.gradle.utils.withDependencies_
import org.jetbrains.dokka.testApi.assertDokkaConfigurationEquals
import java.io.File
import kotlin.test.*

class DokkaCollectorTaskTest {

    @Test
    fun buildDokkaConfiguration() {
        val rootProject = ProjectBuilder.builder().build()
        val childProject = ProjectBuilder.builder().withParent(rootProject).build()
        childProject.plugins.apply("org.jetbrains.kotlin.jvm")

        rootProject.allprojects_ {
            plugins.apply("org.jetbrains.dokka")
            tasks.withType<AbstractDokkaTask>().configureEach_ {
                plugins.withDependencies_ { clear() }
            }
            tasks.withType<DokkaTask>().configureEach_ {
                dokkaSourceSets.configureEach_ {
                    classpath.setFrom(emptyList<Any>())
                }
            }
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach_ {
            moduleName.set("custom Module Name")
            outputDirectory.set(File("customOutputDirectory"))
            cacheRoot.set(File("customCacheRoot"))
            failOnWarning.set(true)
            offlineMode.set(true)
        }

        assertTrue(collectorTasks.isNotEmpty(), "Expected at least one collector task")

        collectorTasks.forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertDokkaConfigurationEquals(
                DokkaConfigurationImpl(
                    moduleName = "custom Module Name",
                    outputDir = rootProject.projectDir.resolve("customOutputDirectory"),
                    cacheRoot = rootProject.projectDir.resolve("customCacheRoot"),
                    failOnWarning = true,
                    offlineMode = true,
                    sourceSets = task.childDokkaTasks
                        .map { it.buildDokkaConfiguration() }
                        .map { it.sourceSets }
                        .reduce { acc, list -> acc + list },
                    pluginsClasspath = task.childDokkaTasks
                        .map { it.plugins.resolve().toList() }
                        .reduce { acc, mutableSet -> acc + mutableSet }
                ),
                dokkaConfiguration,
            )
        }
    }

    @Test
    fun `verify that cacheRoot is optional, and not required to build DokkaConfiguration`() {
        val rootProject = ProjectBuilder.builder().build()
        val childProject = ProjectBuilder.builder().withParent(rootProject).build()
        childProject.plugins.apply("org.jetbrains.kotlin.jvm")

        rootProject.allprojects_ {
            plugins.apply("org.jetbrains.dokka")
            tasks.withType<AbstractDokkaTask>().configureEach_ {
                plugins.withDependencies_ { clear() }
            }
            tasks.withType<DokkaTask>().configureEach_ {
                dokkaSourceSets.configureEach_ {
                    classpath.setFrom(emptyList<Any>())
                }
            }
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach_ {
            cacheRoot.set(null as File?)
        }

        assertTrue(collectorTasks.isNotEmpty(), "Expected at least one collector task")

        collectorTasks.forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertNull(dokkaConfiguration.cacheRoot, "Expect that cacheRoot is null")
        }
    }

    @Test
    fun `with no child tasks throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val collectorTask = project.tasks.create<DokkaCollectorTask>("collector")
        project.configurations.all_ { withDependencies_ { clear() } }
        assertFailsWith<DokkaException> { collectorTask.generateDocumentation() }
    }
}
