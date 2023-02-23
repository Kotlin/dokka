package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaException
import java.io.File
import kotlin.test.*

class DokkaCollectorTaskTest {

    @Test
    fun buildDokkaConfiguration() {
        val rootProject = ProjectBuilder.builder().build()
        val childProject = ProjectBuilder.builder().withParent(rootProject).build()
        childProject.plugins.apply("org.jetbrains.kotlin.jvm")

        rootProject.allprojects {
            plugins.apply("org.jetbrains.dokka")
            tasks.withType<AbstractDokkaTask>().configureEach {
                plugins.withDependencies { clear() }
            }
            tasks.withType<DokkaTask>().configureEach {
                dokkaSourceSets.configureEach {
                    classpath.setFrom(emptyList<Any>())
                }
            }
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach {
            moduleName.set("custom Module Name")
            outputDirectory.set(File("customOutputDirectory"))
            cacheRoot.set(File("customCacheRoot"))
            failOnWarning.set(true)
            offlineMode.set(true)
        }

        assertTrue(collectorTasks.isNotEmpty(), "Expected at least one collector task")

        collectorTasks.forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertEquals(
                DokkaConfigurationImpl(
                    moduleName = "custom Module Name",
                    outputDir = File("customOutputDirectory"),
                    cacheRoot = File("customCacheRoot"),
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

        rootProject.allprojects {
            plugins.apply("org.jetbrains.dokka")
            tasks.withType<AbstractDokkaTask>().configureEach {
                plugins.withDependencies { clear() }
            }
            tasks.withType<DokkaTask>().configureEach {
                dokkaSourceSets.configureEach {
                    classpath.setFrom(emptyList<Any>())
                }
            }
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach {
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
        project.configurations.all { withDependencies { clear() } }
        assertFailsWith<DokkaException> { collectorTask.generateDocumentation() }
    }
}
