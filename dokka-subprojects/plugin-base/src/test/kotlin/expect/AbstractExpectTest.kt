/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package expect

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class AbstractExpectTest(
    val testDir: Path? = Paths.get("src/test", "resources", "expect"),
    val formats: List<String> = listOf("html")
) : BaseAbstractTest() {

    protected fun generateOutput(path: Path, outFormat: String): Path? {
        val config = dokkaConfiguration {
            format = outFormat
            sourceSets {
                sourceSet {
                    sourceRoots = listOf(path.toAbsolutePath().asString())
                }
            }
        }

        var result: Path? = null
        testFromData(config, cleanupOutput = false) {
            renderingStage = { _, context -> result = context.configuration.outputDir.toPath() }
        }
        return result
    }

    protected fun compareOutput(expected: Path, obtained: Path?, gitTimeout: Long = 500) {
        obtained?.let { path ->
            val gitCompare = ProcessBuilder(
                "git",
                "--no-pager",
                "diff",
                expected.asString(),
                path.asString()
            ).also { logger.info("git diff command: ${it.command().joinToString(" ")}") }
                .also { it.redirectErrorStream() }.start()

            assertTrue(gitCompare.waitFor(gitTimeout, TimeUnit.MILLISECONDS), "Git timed out after $gitTimeout")
            gitCompare.inputStream.bufferedReader().lines().forEach { logger.info(it) }
            assertEquals(0, gitCompare.exitValue(), "${path.fileName}: outputs don't match")
        } ?: throw AssertionError("obtained path is null")
    }

    protected fun compareOutputWithExcludes(
        expected: Path,
        obtained: Path?,
        excludes: List<String>,
        timeout: Long = 500
    ) {
        obtained?.let { _ ->
            val (res, out, err) = runDiff(expected, obtained, excludes, timeout)
            assertEquals(0, res, "Outputs differ:\nstdout - $out\n\nstderr - ${err ?: ""}")
        } ?: throw AssertionError("obtained path is null")
    }

    protected fun runDiff(exp: Path, obt: Path, excludes: List<String>, timeout: Long): ProcessResult =
        ProcessBuilder().command(
            listOf("diff", "-ru") + excludes.flatMap { listOf("-x", it) } + listOf("--", exp.asString(), obt.asString())
        ).also {
            it.redirectErrorStream()
        }.start().also { assertTrue(it.waitFor(timeout, TimeUnit.MILLISECONDS), "diff timed out") }.let {
            ProcessResult(it.exitValue(), it.inputStream.bufferResult())
        }


    protected fun testOutput(p: Path, outFormat: String) {
        val expectOut = p.resolve("out/$outFormat")
        val testOut = generateOutput(p.resolve("src"), outFormat)
            .also { logger.info("Test out: ${it?.asString()}") }

        compareOutput(expectOut.toAbsolutePath(), testOut?.toAbsolutePath())
        testOut?.deleteRecursively()
    }

    protected fun testOutputWithExcludes(
        p: Path,
        outFormat: String,
        ignores: List<String> = emptyList(),
        timeout: Long = 500
    ) {
        val expected = p.resolve("out/$outFormat")
        generateOutput(p.resolve("src"), outFormat)
            ?.let { obtained ->
                compareOutputWithExcludes(expected, obtained, ignores, timeout)

                obtained.deleteRecursively()
            } ?: throw AssertionError("Output not generated for ${p.fileName}")
    }

    protected fun generateExpect(p: Path, outFormat: String) {
        val out = p.resolve("out/$outFormat/")
        Files.createDirectories(out)

        val ret = generateOutput(p.resolve("src"), outFormat)
        Files.list(out).forEach { it.deleteRecursively() }
        ret?.let { Files.list(it).forEach { f -> f.copyRecursively(out.resolve(f.fileName)) } }
    }

}
