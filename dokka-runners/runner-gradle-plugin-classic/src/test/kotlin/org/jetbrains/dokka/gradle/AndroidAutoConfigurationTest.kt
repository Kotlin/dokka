/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.*

class AndroidAutoConfigurationTest {

    // Lazy because it throws an error if an Android test is run under Java 8
    private val project by lazy {
        ProjectBuilder.builder().build().also { project ->
            project.plugins.apply("com.android.library")
            project.plugins.apply("org.jetbrains.kotlin.android")
            project.plugins.apply("org.jetbrains.dokka")
            project.extensions.configure<LibraryExtension> {
                compileSdkVersion(28)
            }
        }
    }

    @Test
    @Ignore("Current version of AGP requiers Java 11. TODO: if expected, disable this test for Java 8")
    fun `at least one dokka task created`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        assertTrue(dokkaTasks.isNotEmpty(), "Expected at least one dokka task")
    }

    @Test
    @Ignore("Current version of AGP requiers Java 11. TODO: if expected, disable this test for Java 8")
    fun `all default source sets are present in dokka`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        dokkaTasks.forEach { task ->
            val sourceSets = task.dokkaSourceSets.toList()
            assertEquals(
                listOf(
                    "androidTest", "androidTestDebug", "debug", "main",
                    "release", "test", "testDebug", "testRelease", "androidTestRelease"
                ).sorted(),
                sourceSets.map { it.name }.sorted(),
                "Expected all default source sets being registered"
            )
        }
    }

    @Ignore // TODO: find where `maven` plugin is used, which was removed in Gradle 8
    @Test
    fun `test source sets are suppressed`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        val projectInternal = (project as ProjectInternal)
        projectInternal.evaluate()
        dokkaTasks.flatMap { it.dokkaSourceSets }.forEach { sourceSet ->
            if ("test" in sourceSet.name.toLowerCase()) {
                assertTrue(
                    sourceSet.suppress.get(),
                    "Expected source set `${sourceSet.name}` to be suppressed by default"
                )
            } else {
                assertFalse(
                    sourceSet.suppress.get(),
                    "Expected source set `${sourceSet.name}`to not be suppressed by default"
                )
            }
        }
    }

    @Ignore // TODO: find where `maven` plugin is used, which was removed in Gradle 8
    @Test
    fun `source sets have non-empty classpath`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        val projectInternal = (project as ProjectInternal)
        projectInternal.evaluate()

        dokkaTasks.flatMap { it.dokkaSourceSets }
            .filterNot { it.name == "androidTestRelease" && it.suppress.get() } // androidTestRelease has empty classpath, but it makes no sense for suppressed source set
            .forEach { sourceSet ->
                /*

                There is no better way of checking for empty classpath at the moment (without resolving dependencies).
                We assume, that an empty classpath can be resolved
                We assume, that a non-empty classpath will not be able to resolve (no repositories defined)
                 */
                assertFailsWith<ResolveException>("SourceSet: " + sourceSet.name) { sourceSet.classpath.files }
            }
    }
}
