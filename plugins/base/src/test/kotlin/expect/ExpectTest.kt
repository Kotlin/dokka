package expect

import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.junit.Test
import testApi.testRunner.AbstractCoreTest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ExpectTest : AbstractCoreTest() {

    private fun generateOutput(path: Path): Path? {
        val config = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf(path.asString())
                }
            }
        }

        var result: Path? = null
        testFromData(config, cleanupOutput = false) {
            renderingStage = { _, context -> result = Paths.get(context.configuration.outputDir) }
        }
        return result
    }

    private fun compareOutput(expected: Path, obtained: Path?, gitTimeout: Long = 500) {
        obtained?.let { path ->
            val gitCompare = ProcessBuilder(
                "git",
                "--no-pager",
                "diff",
                expected.asString(),
                path.asString()
            ).also { DokkaConsoleLogger.info("git diff command: ${it.command().joinToString(" ")}") }
                .start()

            assert(gitCompare.waitFor(gitTimeout, TimeUnit.MILLISECONDS)) { "Git timed out after $gitTimeout" }
            gitCompare.inputStream.bufferedReader().lines().forEach { DokkaConsoleLogger.info(it) }
            gitCompare.errorStream.bufferedReader().lines().forEach { DokkaConsoleLogger.info(it) }
            assert(gitCompare.exitValue() == 0) { "${path.fileName}: outputs don't match" }
        } ?: throw AssertionError("obtained path is null")
    }

    @Test
    fun expectTest() {
        val logger = DokkaConsoleLogger
        val sources = Paths.get("src/test", "resources", "expect")

        Files.list(sources).forEach { p ->
            val expectOut = p.resolve("out")
            val testOut = generateOutput(p.resolve("src"))
                .also { logger.info("Test out: ${it?.asString()}") }

            compareOutput(expectOut, testOut)
        }
    }

    fun Path.asString() = toAbsolutePath().normalize().toString()

}