package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaTaskTest {
    @Test
    fun `no suppressed source sets are present after in built configuration`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create<DokkaTask>("dokkaTask")
        project.configurations.all { configuration -> configuration.withDependencies { it.clear() } }

        task.dokkaSourceSets.register("main")
        task.dokkaSourceSets.register("jvm")
        task.dokkaSourceSets.register("test") {
            it.suppress by true
        }

        assertEquals(
            listOf("main", "jvm").sorted(),
            task.buildDokkaConfiguration().sourceSets.map { it.sourceSetID.sourceSetName }.sorted(),
            "Expected only unsuppressed source sets `main` and `test` to be present in built configuration"
        )
    }
}
