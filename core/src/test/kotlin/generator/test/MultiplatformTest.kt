package generator.test

import generator.TestRunner
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import utils.Builders

class MultiplatformTest {

    companion object {
        @AfterClass @JvmStatic
        fun cleanup() =
            TestRunner.cleanup()
    }

    val platforms = Platform.values().map { it.name }

    val configBuilder = Builders.ConfigBuilder(
        format = "html",
        generateIndexPages = true
    )
    val passBuilder = Builders.PassBuilder(targets = platforms)

    val name: TestName = TestName()
    @Rule
    fun name(): TestName = name

    val logger = DokkaConsoleLogger

    @Test
    fun example() {
        val testName = name.methodName

        val passBuilders =
            TestRunner.generatePassesForPlatforms(testName, platforms, passBuilder)

        TestRunner.testFromSourceSets(
            name = testName,
            configBuilder = configBuilder.copy(passesConfigurations = passBuilders),
            pagesTransformationTest = { assert(it.children.size == 2) },
            finalTest = {
              assert(it.passesConfigurations.size == 3)
            },
            logger = logger
        )
    }

}