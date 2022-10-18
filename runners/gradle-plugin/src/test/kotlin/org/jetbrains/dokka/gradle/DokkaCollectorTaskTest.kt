package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DokkaCollectorTaskTest {

    @Test
    fun buildDokkaConfiguration() {
        val rootProject = ProjectBuilder.builder().build()
        val childProject = ProjectBuilder.builder().withParent(rootProject).build()
        childProject.plugins.apply("org.jetbrains.kotlin.jvm")

        rootProject.allprojects { project ->
            project.plugins.apply("org.jetbrains.dokka")
            project.tasks.withType<AbstractDokkaTask>().configureEach { task ->
                task.plugins.withDependencies { dependencies -> dependencies.clear() }
            }
            project.tasks.withType<DokkaTask>().configureEach { task ->
                task.dokkaSourceSets.configureEach { sourceSet ->
                    sourceSet.classpath.setFrom(emptyList<Any>())
                }
            }
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach { task ->
            task.moduleName by "custom Module Name"
            task.outputDirectory by File("customOutputDirectory")
            task.cacheRoot by File("customCacheRoot")
            task.failOnWarning by true
            task.offlineMode by true
        }

        assertTrue(collectorTasks.isNotEmpty(), "Expected at least one collector task")

        collectorTasks.toList().forEach { task ->
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
                dokkaConfiguration
            )
        }
    }

    @Test
    fun `with no child tasks throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val collectorTask = project.tasks.create<DokkaCollectorTask>("collector")
        project.configurations.all { configuration -> configuration.withDependencies { it.clear() } }
        assertFailsWith<DokkaException> { collectorTask.generateDocumentation() }
    }
}
