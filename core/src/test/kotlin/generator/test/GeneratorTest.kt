package generator.test

import generator.ConfigParams
import generator.TestRunner
import org.jetbrains.dokka.*
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import utils.Builders

class GeneratorTest {

    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = TestRunner.cleanup()
    }

    val builder = Builders.ConfigBuilder(
        format = "html",
        generateIndexPages = true
    )

    val name: TestName = TestName()
    @Rule
    fun name(): TestName = name

    val logger = DokkaConsoleLogger
    fun config(): (ConfigParams) -> DokkaConfiguration = { (name, root, out) ->
        builder.copy(
            passesConfigurations = listOf(
                Builders.PassBuilder(
                    moduleName = name,
                    sourceRoots = listOf(root.toString()),
                    analysisPlatform = "jvm",
                    targets = listOf("jvm")
                )
            )
        )(out.toString())
    }

    @Test
    fun test1() {
        TestRunner.testInline(
            name = name.methodName,
            query = """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            pagesCreationTest = { pages ->
                val test = pages.parentMap.size == 7
                if (!test) println(pages.parentMap.size)
                assert(test)
            }, config = config(), logger = logger
        )
    }
}