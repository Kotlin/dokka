/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.inspectors.shouldForNone
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.sequences.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.*

/**
 * Small reproducer for issue reported by kotlinx-io
 * https://github.com/Kotlin/dokka/issues/4116
 */
class KmpSharedWasmTest : FunSpec({
    context("given a KMP project with a custom shared Wasm source set") {
        val project = initKmpSharedWasmProject()

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--stacktrace",
            )
            .forwardOutput()
            .build {

                test("expect project builds successfully") {
                    output shouldContain "BUILD SUCCESSFUL"
                }

                test("expect no 'unknown class' message in HTML files") {
                    val htmlFiles = project.projectDir
                        .resolve("build/dokka/html")
                        .walk()
                        .filter { it.isRegularFile() && it.extension == "html" }

                    htmlFiles.shouldNotBeEmpty()

                    htmlFiles.forEach { htmlFile ->
                        htmlFile.relativeTo(project.projectDir).apply {
                            shouldNotContainText("Error class: unknown class")
                            shouldNotContainText("ERROR CLASS: Symbol not found")
                        }
                    }
                }

                test("expect all Dokka workers are successful") {
                    project
                        .findFiles { it.name == "dokka-worker.log" }
                        .shouldForAll { dokkaWorkerLog ->
                            dokkaWorkerLog.shouldBeAFile()
                            dokkaWorkerLog.useLines { lines ->
                                lines.shouldForNone { line ->
                                    line.startsWith("[ERROR]") || line.startsWith("[WARN]")
                                }
                            }
                        }
                }
            }
    }
})

private fun initKmpSharedWasmProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("kmp-shared-wasm") {

        settingsGradleKts += """
            |
            """.trimMargin()

        buildGradleKts = """
            |plugins {
            |  kotlin("multiplatform") version embeddedKotlinVersion
            |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
            |}
            |
            |@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
            |kotlin {
            |  wasmJs { browser() }
            |  wasmWasi { nodejs() }
            |
            |  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
            |  applyDefaultHierarchyTemplate {
            |    common {
            |      group("wasm") {
            |        withWasmJs()
            |        withWasmWasi()
            |      }
            |    }
            |  }
            |}
            |""".trimMargin()


        dir("src/commonMain/kotlin") {
            createKotlinFile(
                "IOException.kt",
                """
                |/**
                | * Signals about a general issue occurred during I/O operation.
                | */
                |expect open class IOException : Exception {
                |  constructor()
                |  constructor(message: String?)
                |  constructor(cause: Throwable?)
                |  constructor(message: String?, cause: Throwable?)
                |}
                |""".trimMargin()
            )

            createKotlinFile(
                "RawSink.kt",
                """
                |/**
                | * Receives a stream of bytes. RawSink is a base interface for `kotlinx-io` data receivers.
                | *
                | * This interface should be implemented to write data wherever it's needed: to the network, storage,
                | * or a buffer in memory. Sinks may be layered to transform received data, such as to compress, encrypt, throttle,
                | * or add protocol framing.
                | *
                | * Most application code shouldn't operate on a raw sink directly, but rather on a buffered [Sink] which
                | * is both more efficient and more convenient. Use [buffered] to wrap any raw sink with a buffer.
                | *
                | * Implementors should abstain from throwing exceptions other than those that are documented for RawSink methods.
                | *
                | * ### Thread-safety guarantees
                | *
                | * [RawSink] implementations are not required to be thread safe.
                | * However, if an implementation provides some thread safety guarantees, it is recommended to explicitly document them.
                | */
                |expect interface RawSink : AutoCloseable {
                |  /**
                |   * Removes [byteCount] bytes from [source] and appends them to this sink.
                |   *
                |   * @param source the source to read data from.
                |   * @param byteCount the number of bytes to write.
                |   *
                |   * @throws IllegalArgumentException when the [source]'s size is below [byteCount] or [byteCount] is negative.
                |   * @throws IllegalStateException when the sink is closed.
                |   * @throws IOException when some I/O error occurs.
                |   */
                |  fun write(source: Buffer, byteCount: Long)
                |
                |  /**
                |   * Pushes all buffered bytes to their final destination.
                |   *
                |   * @throws IllegalStateException when the sink is closed.
                |   * @throws IOException when some I/O error occurs.
                |   */
                |  fun flush()
                |
                |  /**
                |   * Pushes all buffered bytes to their final destination and releases the resources held by this
                |   * sink. It is an error to write a closed sink. It is safe to close a sink more than once.
                |   *
                |   * @throws IOException when some I/O error occurs.
                |   */
                |  override fun close()
                |}
                |""".trimMargin()
            )

            createKotlinFile(
                "Buffer.kt", """
                |interface Buffer
                |""".trimMargin()
            )
        }
        dir("src/wasmMain/kotlin") {
            createKotlinFile(
                "IOException.wasm.kt",
                """
                |actual open class IOException : Exception {
                |  actual constructor() : super()
                |  actual constructor(message: String?) : super(message)
                |  actual constructor(cause: Throwable?) : super(cause)
                |  actual constructor(message: String?, cause: Throwable?) : super(message, cause)
                |}
                |""".trimMargin()
            )

            createKotlinFile(
                "RawSink.wasm.kt",
                """
                |actual interface RawSink : AutoCloseable {
                |  actual fun write(source: Buffer, byteCount: Long)
                |
                |  actual fun flush()
                |
                |  actual override fun close()
                |}
                |""".trimMargin()
            )
        }

        config()
    }
}
