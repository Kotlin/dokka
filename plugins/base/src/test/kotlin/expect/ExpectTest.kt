package expect

import org.junit.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
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
            ).also { logger.info("git diff command: ${it.command().joinToString(" ")}") }
                .start()

            assert(gitCompare.waitFor(gitTimeout, TimeUnit.MILLISECONDS)) { "Git timed out after $gitTimeout" }
            gitCompare.inputStream.bufferedReader().lines().forEach { logger.info(it) }
            gitCompare.errorStream.bufferedReader().lines().forEach { logger.info(it) }
            assert(gitCompare.exitValue() == 0) { "${path.fileName}: outputs don't match" }
        } ?: throw AssertionError("obtained path is null")
    }

    @Test
    fun expectTest() {
        val sources = Paths.get("src/test", "resources", "expect")

        Files.list(sources).forEach { p ->
            val expectOut = p.resolve("out")
            val testOut = generateOutput(p.resolve("src"))
                .also { logger.info("Test out: ${it?.asString()}") }

            compareOutput(expectOut, testOut)
            testOut?.toFile()?.deleteRecursively()
        }
    }

    fun Path.asString() = toAbsolutePath().normalize().toString()

}