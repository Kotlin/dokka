package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.CompactInParent
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout.NoCopy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DokkaMultiModuleFileLayoutTest {

    @Test
    fun `no copy`() {
        val project = ProjectBuilder.builder().build()
        val child = project.tasks.create<DokkaTask>("child")
        val parent = project.tasks.create<DokkaMultiModuleTask>("parent")
        child.outputDirectory by File("some/path")

        assertEquals(
            File("some/path"), NoCopy.targetChildOutputDirectory(parent, child),
            "Expected original file path returned"
        )
    }

    @Test
    fun `compact in parent`() {
        val rootProject = ProjectBuilder.builder().build()
        val parentProject = ProjectBuilder.builder().withName("parent").withParent(rootProject).build()
        val intermediateProject = ProjectBuilder.builder().withName("intermediate").withParent(parentProject).build()
        val childProject = ProjectBuilder.builder().withName("child").withParent(intermediateProject).build()

        val parentTask = parentProject.tasks.create<DokkaMultiModuleTask>("parentTask")
        val childTask = childProject.tasks.create<DokkaTask>("childTask")

        val targetOutputDirectory = CompactInParent.targetChildOutputDirectory(parentTask, childTask)
        assertEquals(
            parentTask.outputDirectory.getSafe().resolve("intermediate/child"), targetOutputDirectory,
            "Expected nested file structure representing project structure"
        )
    }

    @Test
    fun copyChildOutputDirectory() {
        /* Prepare */
        val project = ProjectBuilder.builder().build()
        val childTask = project.tasks.create<DokkaTask>("child")
        val parentTask = project.tasks.create<DokkaMultiModuleTask>("parent")

        val sourceOutputDirectory = childTask.outputDirectory.getSafe()
        sourceOutputDirectory.mkdirs()
        sourceOutputDirectory.resolve("some.file").writeText("some text")
        val subFolder = sourceOutputDirectory.resolve("subFolder")
        subFolder.mkdirs()
        subFolder.resolve("other.file").writeText("other text")

        parentTask.fileLayout by object : DokkaMultiModuleFileLayout {
            override fun targetChildOutputDirectory(parent: DokkaMultiModuleTask, child: AbstractDokkaTask): File {
                return parent.project.file("target/output")
            }
        }
        parentTask.copyChildOutputDirectory(childTask)

        /* Assertions */
        val targetOutputDirectory = project.file("target/output")
        assertTrue(
            targetOutputDirectory.exists() && targetOutputDirectory.isDirectory,
            "Expected target output directory ${targetOutputDirectory.path} to exist"
        )

        val targetSomeFile = targetOutputDirectory.resolve("some.file")
        assertTrue(
            targetSomeFile.exists() && targetSomeFile.isFile,
            "Expected sample file to exist in target output directory"
        )

        assertEquals(
            "some text", targetSomeFile.readText(),
            "Expected content to be written into sample file"
        )

        val targetSubFolder = targetOutputDirectory.resolve("subFolder")
        assertTrue(
            targetSubFolder.exists() && targetSubFolder.isDirectory,
            "Expected sub folder being present in target output directory"
        )

        val targetOtherFile = targetSubFolder.resolve("other.file")
        assertTrue(
            targetOtherFile.exists() && targetOtherFile.isFile,
            "Expected nested 'other.file' being copied into target"
        )

        assertEquals(
            "other text", targetOtherFile.readText(),
            "Expected content to be written into 'other.file'"
        )
    }

    @Test
    fun `copyChildOutputDirectory target output directory within itself throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val childTask = project.tasks.create<DokkaTask>("child")
        val parentTask = project.tasks.create<DokkaMultiModuleTask>("parent")
        parentTask.fileLayout by object : DokkaMultiModuleFileLayout {
            override fun targetChildOutputDirectory(parent: DokkaMultiModuleTask, child: AbstractDokkaTask): File {
                return child.outputDirectory.getSafe().resolve("subfolder")
            }
        }
        assertFailsWith<DokkaException> { parentTask.copyChildOutputDirectory(childTask) }
    }

    @Test
    fun `copyChildOutputDirectory NoCopy`() {
        val project = ProjectBuilder.builder().build()
        val childTask = project.tasks.create<DokkaTask>("child")
        val parentTask = project.tasks.create<DokkaMultiModuleTask>("parent")
        parentTask.fileLayout by NoCopy
        parentTask.copyChildOutputDirectory(childTask)
    }
}
