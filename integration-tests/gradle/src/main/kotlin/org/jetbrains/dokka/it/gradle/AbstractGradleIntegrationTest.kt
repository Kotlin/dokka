package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class AbstractGradleIntegrationTest {

    @get:Rule
    val projectTemporaryFolder = TemporaryFolder()

    val projectPath get() = projectTemporaryFolder.root.toPath()

    val projectDir get() = projectTemporaryFolder.root


    fun createGradleRunner(
        buildVersions: BuildVersions, arguments: Array<String>
    ): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withGradleVersion(buildVersions.gradleVersion)
            .forwardOutput()
            .withArguments("-Pkotlin_version=${buildVersions.kotlinVersion}", *arguments)
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

