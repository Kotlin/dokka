package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbstractDokkaParentTaskTest {

    private val rootProject = ProjectBuilder.builder().build()
    private val subproject0 = ProjectBuilder.builder().withName("subproject0").withParent(rootProject).build()
    private val subproject1 = ProjectBuilder.builder().withName("subproject1").withParent(rootProject).build()
    private val subSubproject0a = ProjectBuilder.builder().withName("subSubproject0a").withParent(subproject0).build()

    init {
        rootProject.allprojects { project -> project.plugins.apply("org.jetbrains.dokka") }
    }

    private val parentTasks = rootProject.tasks.withType<AbstractDokkaParentTask>().toList()

    @Test
    fun `at least one parent task is registered`() {
        assertTrue(
            parentTasks.isNotEmpty(),
            "Expected at least one ${AbstractDokkaParentTask::class.simpleName} task in rootProject"
        )
    }

    @Test
    fun `configuring subprojects`() {
        parentTasks.forEach { task ->
            assertEquals(
                setOf(":subproject0", ":subproject1", ":subproject0:subSubproject0a"), task.subprojectPaths,
                "Expected all sub projects registered by default"
            )

            assertEquals(
                listOf(subproject0, subproject1, subSubproject0a), task.subprojects,
                "Expected all sub projects registered by default"
            )

            assertEquals(3, task.dokkaTasks.size, "Expected three referenced dokka tasks")
            assertTrue(listOf(subproject0, subproject1, subSubproject0a).all { project ->
                task.dokkaTasks.any { task -> task in project.tasks }
            }, "Expected all sub projects to contribute to referenced dokka tasks")

            task.removeSubproject(subproject0)
            assertEquals(
                setOf(":subproject1", ":subproject0:subSubproject0a"), task.subprojectPaths,
                "Expected subproject0 to be removed (without removing its children)"
            )

            task.addSubproject(subproject0)
            assertEquals(
                setOf(":subproject1", ":subproject0:subSubproject0a", ":subproject0"), task.subprojectPaths,
                "Expected subproject0 being added again"
            )

            task.addSubproject(subproject0)
            assertEquals(
                setOf(":subproject1", ":subproject0:subSubproject0a", ":subproject0"), task.subprojectPaths,
                "Expected adding same project twice to be ignored"
            )

            task.removeAllProjects(subproject0)
            assertEquals(
                setOf(":subproject1"), task.subprojectPaths,
                "Expected subproject0 and subSubproject0a to be removed"
            )

            task.addAllProjects(subproject0)
            assertEquals(
                setOf(":subproject1", ":subproject0", ":subproject0:subSubproject0a"), task.subprojectPaths,
                "Expected subproject0 and subSubproject0a to be added again"
            )
        }
    }

    @Test
    fun `configure dokkaTaskNames`() {
        parentTasks.forEach { task ->
            assertEquals(
                3, task.dokkaTasks.size,
                "Expected 3 tasks referenced by default"
            )

            val customDokkaTaskName = "custom${task.name}"
            task.dokkaTaskNames = setOf(customDokkaTaskName)
            assertTrue(task.dokkaTasks.isEmpty(), "Expected no $customDokkaTaskName. Found: ${task.dokkaTasks}")

            rootProject.subprojects { subproject ->
                subproject.tasks.register<DokkaTask>(customDokkaTaskName)
            }

            assertEquals(
                3, task.dokkaTasks.size,
                "Expected three $customDokkaTaskName found"
            )

            task.dokkaTasks.forEach { dokkaTask ->
                assertEquals(customDokkaTaskName, dokkaTask.name)
            }
        }
    }
}
