package org.jetbrains.dokka.gradle

import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DokkaPluginApplyTest {

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
        assertDokkaTasksHaveDocumentationGroup(project.tasks)
    }

    @Test
    fun `all dokka tasks are part of the documentation group in a multi module setup`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val child = ProjectBuilder.builder().withName("child").withParent(root).build()
        root.plugins.apply("org.jetbrains.dokka")
        child.plugins.apply("org.jetbrains.dokka")
        assertDokkaTasksHaveDocumentationGroup(root.tasks)
        assertDokkaTasksHaveDocumentationGroup(child.tasks)
    }

    @Test
    fun `old dokka tasks are part of the deprecated group in a multi module setup`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val child = ProjectBuilder.builder().withName("child").withParent(root).build()
        root.plugins.apply("org.jetbrains.dokka")
        child.plugins.apply("org.jetbrains.dokka")
        assertOldDokkaTasksHaveDeprecatedGroup(root.tasks)
        assertOldDokkaTasksHaveDeprecatedGroup(child.tasks)
    }

    @Test
    fun `all dokka tasks provide a task description`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        assertDokkaTasksHaveDescription(project.tasks)
    }

    @Test
    fun `all dokka tasks provide a task description in a multi module setup`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val child = ProjectBuilder.builder().withName("child").withParent(root).build()
        root.plugins.apply("org.jetbrains.dokka")
        child.plugins.apply("org.jetbrains.dokka")
        assertDokkaTasksHaveDescription(root.tasks)
        assertDokkaTasksHaveDescription(child.tasks)
    }

    @Test
    fun `parent dokka tasks have children configured`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val child = ProjectBuilder.builder().withName("child").withParent(root).build()
        root.plugins.apply("org.jetbrains.dokka")
        child.plugins.apply("org.jetbrains.dokka")

        val parentTasks = root.tasks.withType<AbstractDokkaParentTask>()
        assertTrue(parentTasks.isNotEmpty(), "Expected at least one parent task being created")

        parentTasks.toList().forEach { parentTask ->
            assertEquals(1, parentTask.childDokkaTasks.size, "Expected one child dokka task")
            assertEquals(
                child, parentTask.childDokkaTasks.single().project,
                "Expected child dokka task from child project"
            )
        }
    }
}

private fun assertDokkaTasksHaveDocumentationGroup(taskContainer: TaskContainer) {
    taskContainer.withType<AbstractDokkaTask>().forEach { dokkaTask ->
        assertEquals(
            JavaBasePlugin.DOCUMENTATION_GROUP,
            dokkaTask.group,
            "Expected task: ${dokkaTask.path} group to be \"${JavaBasePlugin.DOCUMENTATION_GROUP}\""
        )
    }
}

private fun assertOldDokkaTasksHaveDeprecatedGroup(taskContainer: TaskContainer) {
    taskContainer.names.filter { "Multimodule" in it }.forEach { dokkaTaskName ->
        val dokkaTask = taskContainer.getByName(dokkaTaskName)
        val expectedGroup = "deprecated"
        assertEquals(
            expectedGroup,
            dokkaTask.group,
            "Expected task: ${dokkaTask.path} group to be \"${expectedGroup}\""
        )
    }
}

private fun assertDokkaTasksHaveDescription(taskContainer: TaskContainer) {
    taskContainer.withType<AbstractDokkaTask>().forEach { dokkaTask ->
        assertTrue(
            @Suppress("UselessCallOnNotNull") // Task.description is nullable, but not inherited as Kotlin sees it.
            dokkaTask.description.orEmpty().isNotEmpty(),
            "Expected description for task ${dokkaTask.name}"
        )
    }
}
