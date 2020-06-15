package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.BeforeTest

@RunWith(Parameterized::class)
abstract class AbstractGradleIntegrationTest : AbstractIntegrationTest() {

    abstract val versions: BuildVersions

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
            .withJvmArguments("-Xmx4G", "-XX:MaxMetaspaceSize=1G")
    }
}
