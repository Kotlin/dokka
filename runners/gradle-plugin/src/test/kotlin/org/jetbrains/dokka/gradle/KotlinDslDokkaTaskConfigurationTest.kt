package org.jetbrains.dokka.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinDslDokkaTaskConfigurationTest {

    @Test
    fun `configure project using dokka extension function`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        project.dokka { this.outputFormat = "test" }

        project.tasks.withType(DokkaTask::class.java).forEach { dokkaTask ->
            assertEquals("test", dokkaTask.outputFormat)
        }
    }
}
