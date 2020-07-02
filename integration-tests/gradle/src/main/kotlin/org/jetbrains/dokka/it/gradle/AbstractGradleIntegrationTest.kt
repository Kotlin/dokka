package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertFalse

abstract class AbstractGradleIntegrationTest {

    @get:Rule
    val temporaryTestFolder = TemporaryFolder()

    val projectDir get() = File(temporaryTestFolder.root, "project")

    val projectPath get() = projectDir.toPath()

    @BeforeTest
    fun copyTemplates() {
        File("projects").listFiles().orEmpty()
            .filter { it.isFile }
            .filter { it.name.startsWith("template.") }
            .forEach { file -> file.copyTo(File(temporaryTestFolder.root, file.name)) }
    }

    fun createGradleRunner(
        buildVersions: BuildVersions, arguments: Array<String>
    ): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withGradleVersion(buildVersions.gradleVersion)
            .forwardOutput()
            .withArguments(
                "-Pkotlin_version=${buildVersions.kotlinVersion}",
                "-Pdokka_it_kotlin_version=${buildVersions.kotlinVersion}",
                * arguments
            )
            .withDebug(true)

    }

    protected fun assertContainsNoErrorClass(file: File) {
        val fileText = file.readText()
        assertFalse(
            fileText.contains("ERROR CLASS", ignoreCase = true),
            "Unexpected `ERROR CLASS` in ${file.path}\n" + fileText
        )
    }

    protected fun assertNoUnresolvedLInks(file: File) {
        val regex = Regex("[\"']#[\"']")
        val fileText = file.readText()
        assertFalse(
            fileText.contains(regex),
            "Unexpected unresolved link in ${file.path}\n" + fileText
        )
    }
}

