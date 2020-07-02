package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

@RunWith(Parameterized::class)
abstract class AbstractGradleIntegrationTest {

    abstract val versions: BuildVersions

    @get:Rule
    val temporaryTestFolder = TemporaryFolder()

    val projectDir get() = File(temporaryTestFolder.root, "project")

    @BeforeTest
    fun copyTemplates() {
        File("projects").listFiles().orEmpty()
            .filter { it.isFile }
            .filter { it.name.startsWith("template.") }
            .forEach { file -> file.copyTo(File(temporaryTestFolder.root, file.name)) }
    }

    fun createGradleRunner(
        vararg arguments: String
    ): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withGradleVersion(versions.gradleVersion.version)
            .forwardOutput()
            .withTestKitDir(File("build", "gradle-test-kit").absoluteFile)
            .withArguments(
                listOfNotNull(
                    "-Pkotlin_version=${versions.kotlinVersion}",
                    "-Pdokka_it_kotlin_version=${versions.kotlinVersion}",
                    versions.androidGradlePluginVersion?.let { androidVersion ->
                        "-Pdokka_it_android_gradle_plugin_version=$androidVersion"
                    },
                    * arguments
                )
            ).run { this as DefaultGradleRunner }
            .withJvmArguments("-Xmx4G", "-XX:MaxMetaspaceSize=512M")
    }

    fun File.allDescendentsWithExtension(extension: String): Sequence<File> {
        return this.walkTopDown().filter { it.isFile && it.extension == extension }
    }

    fun File.allHtmlFiles(): Sequence<File> {
        return allDescendentsWithExtension("html")
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

