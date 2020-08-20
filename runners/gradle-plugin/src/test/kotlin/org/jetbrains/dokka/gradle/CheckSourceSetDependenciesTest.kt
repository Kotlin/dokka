package org.jetbrains.dokka.gradle

import org.gradle.testfixtures.ProjectBuilder
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CheckSourceSetDependenciesTest {

    private val project = ProjectBuilder.builder().build()

    @Test
    fun `passes when properly configured`() {
        val sourceSets = listOf(
            GradleDokkaSourceSetBuilder("common", project),
            GradleDokkaSourceSetBuilder("jvmAndJsCommon", project).apply {
                dependsOn("common")
            },
            GradleDokkaSourceSetBuilder("jvm", project).apply {
                dependsOn("jvmAndJsCommon")
            },
            GradleDokkaSourceSetBuilder("js", project).apply {
                dependsOn("jvmAndJsCommon")
            }
        )
        checkSourceSetDependencies(sourceSets)
    }

    @Test
    fun `throws exception when dependent source set id cant be found`() {
        val sourceSets = listOf(
            GradleDokkaSourceSetBuilder("main", project),
            GradleDokkaSourceSetBuilder("bad", project).apply {
                dependsOn("missing")
            }
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            checkSourceSetDependencies(sourceSets)
        }

        assertTrue("bad" in exception.message.orEmpty(), "Expected name of source set mentioned")
        assertTrue("missing" in exception.message.orEmpty(), "Expected name of missing source set mentioned")
    }

    @Test
    fun `throws exception when documented source set depends on suppressed source set`() {
        val sourceSets = listOf(
            GradleDokkaSourceSetBuilder("common", project),
            GradleDokkaSourceSetBuilder("intermediate", project).apply {
                dependsOn("common")
                suppress by true
            },
            GradleDokkaSourceSetBuilder("jvm", project).apply {
                dependsOn("intermediate")
            }
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            checkSourceSetDependencies(sourceSets)
        }

        assertTrue("intermediate" in exception.message.orEmpty())
        assertTrue("jvm" in exception.message.orEmpty())
    }
}
