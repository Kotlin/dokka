/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.utils.all_
import org.jetbrains.dokka.gradle.utils.register_
import org.jetbrains.dokka.gradle.utils.withDependencies_
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DokkaTaskTest {
    @Test
    fun `no suppressed source sets are present after in built configuration`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        val task = project.tasks.create<DokkaTask>("dokkaTask")
        project.configurations.all_ { withDependencies_ { clear() } }

        task.dokkaSourceSets.register("main")
        task.dokkaSourceSets.register("jvm")
        task.dokkaSourceSets.register_("test") {
            suppress.set(true)
        }

        assertEquals(
            listOf("main", "jvm").sorted(),
            task.buildDokkaConfiguration().sourceSets.map { it.sourceSetID.sourceSetName }.sorted(),
            "Expected only unsuppressed source sets `main` and `test` to be present in built configuration"
        )
    }

    @Test
    fun `module version is not present if not specified`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        val task = project.tasks.create<DokkaTask>("dokkaTask")
        project.configurations.all_ { withDependencies_ { clear() } }

        task.dokkaSourceSets.register("main")
        assertNull(task.buildDokkaConfiguration().moduleVersion)
    }
}
