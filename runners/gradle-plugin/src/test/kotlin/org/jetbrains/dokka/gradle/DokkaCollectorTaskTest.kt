package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
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
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach { task ->
            task.outputDirectory = File("customOutputDirectory")
            task.cacheRoot = File("customCacheRoot")
            task.failOnWarning = true
            task.offlineMode = true
        }

        assertTrue(collectorTasks.isNotEmpty(), "Expected at least one collector task")

        collectorTasks.forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertEquals(
                DokkaConfigurationImpl(
                    outputDir = File("customOutputDirectory"),
                    cacheRoot = File("customCacheRoot"),
                    failOnWarning = true,
                    offlineMode = true,
                    sourceSets = task.dokkaTasks
                        .map { it.buildDokkaConfiguration() }
                        .map { it.sourceSets }
                        .reduce { acc, list -> acc + list },
                    pluginsClasspath = task.dokkaTasks
                        .map { it.plugins.resolve() }
                        .reduce { acc, mutableSet -> acc + mutableSet }
                ),
                dokkaConfiguration
            )
        }

    }
}
