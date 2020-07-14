package org.jetbrains.dokka.gradle

import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DokkaTasksTest {

    @Test
    fun `one task per format is registered`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        assertTrue(
            project.tasks.findByName("dokkaHtml") is DokkaTask,
            "Expected DokkaTask: dokkaHtml"
        )

        assertTrue(
            project.tasks.findByName("dokkaGfm") is DokkaTask,
            "Expected DokkaTask: dokkaGfm"
        )

        assertTrue(
            project.tasks.findByName("dokkaJekyll") is DokkaTask,
            "Expected DokkaTask: dokkaJekyll"
        )

        assertTrue(
            project.tasks.findByName("dokkaJavadoc") is DokkaTask,
            "Expected DokkaTask: dokkaJavadoc"
        )
    }

    @Test
    fun `dokka plugin configurations extend dokkaPlugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        val dokkaPluginsConfiguration = project.maybeCreateDokkaDefaultPluginConfiguration()

        project.tasks.withType<DokkaTask>().forEach { dokkaTask ->
            assertSame(
                dokkaTask.plugins.extendsFrom.single(), dokkaPluginsConfiguration,
                "Expected dokka plugins configuration to extend default ${dokkaPluginsConfiguration.name} configuration"
            )
        }
    }

    @Test
    fun `all dokka tasks are part of the documentation group`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        project.tasks.filter { "dokka" in it.name.toLowerCase() }.forEach { dokkaTask ->
            assertEquals(
                JavaBasePlugin.DOCUMENTATION_GROUP, dokkaTask.group,
                "Expected task: ${dokkaTask.path} group to be ${JavaBasePlugin.DOCUMENTATION_GROUP}"
            )
        }
    }
}
