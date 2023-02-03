package org.jetbrains.dokka.gradle.it

import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.utils.gradleKtsProjectIntegrationTest
import org.jetbrains.dokka.gradle.utils.gradleProperties
import org.junit.jupiter.api.Test

class BasicProjectIntegrationTest {


    private val basicProject = gradleKtsProjectIntegrationTest("it-basic") {
        gradleProperties = gradleProperties.lines().joinToString("\n") { line ->
            if (line.startsWith("testMavenRepoDir")) {
                "testMavenRepoDir=${testMavenRepoRelativePath}"
            } else {
                line
            }
        }
    }

    @Test
    fun `test basic project`() {
        val build = basicProject.runner
            .withArguments(
                "dokkaGenerate",
                "--stacktrace",
                "--info",
//                "-PtestMavenRepoDir=${testMavenRepoDir}",
            )
            .forwardOutput()
            .build()

        build.output shouldContain "BUILD SUCCESSFUL"
        build.output shouldContain "Generation completed successfully"
    }
}
