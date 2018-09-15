package org.jetbrains.dokka.gradle

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TypeSafeConfigurationTest(private val testCase: TestCase) : AbstractDokkaGradleTest() {

    data class TestCase(val gradleVersion: String, val kotlinVersion: String) {
        override fun toString(): String = "Gradle $gradleVersion and Kotlin $kotlinVersion"
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun testCases() = listOf(
                TestCase("4.0", "1.1.2"),
                TestCase("4.5", "1.2.20"),
                TestCase("4.10.1", "1.2.60")
        )
    }

    @Test
    fun test() {

        testDataFolder.resolve("typeSafeConfiguration").toFile()
                .copyRecursively(testProjectDir.root)

        configure(
                testCase.gradleVersion,
                testCase.kotlinVersion,
                arguments = arrayOf("help", "-s")
        ).build()
    }
}
