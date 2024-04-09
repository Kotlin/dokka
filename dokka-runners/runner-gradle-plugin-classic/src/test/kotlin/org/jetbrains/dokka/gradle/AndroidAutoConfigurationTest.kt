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
import org.jetbrains.dokka.gradle.utils.isAgpRunnable
import kotlin.test.*

class AndroidAutoConfigurationTest {

    private val project = ProjectBuilder.builder().build().also { project ->
        if (isAgpRunnable()) {
            project.plugins.apply("com.android.library")
            project.plugins.apply("org.jetbrains.kotlin.android")
            project.plugins.apply("org.jetbrains.dokka")
            project.extensions.configure<LibraryExtension> {
                compileSdk = 28
            }
        }
    }

    @Test
    fun `at least one dokka task created`() {
        if (!isAgpRunnable()) return

        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        assertTrue(dokkaTasks.isNotEmpty(), "Expected at least one dokka task")
    }

    @Test
    fun `all default source sets are present in dokka`() {
        if (!isAgpRunnable()) return

        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        dokkaTasks.forEach { task ->
            val sourceSets = task.dokkaSourceSets.map { it.name }.toSet()
            assertEquals(
                setOf(
                    "main", "debug", "release",
                    "test", "testDebug", "testRelease",
                    "androidTest", "androidTestDebug", "androidTestRelease",
                    "testFixtures", "testFixturesDebug", "testFixturesRelease"
                ),
                sourceSets,
                "Expected all default source sets being registered"
            )
        }
    }

    @Ignore // TODO: find where `maven` plugin is used, which was removed in Gradle 8
    @Test
    fun `test source sets are suppressed`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        project as ProjectInternal
        project.evaluate()
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
        project as ProjectInternal
        project.evaluate()

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
