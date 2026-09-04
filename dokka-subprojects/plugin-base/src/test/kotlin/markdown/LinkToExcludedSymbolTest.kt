/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package markdown

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A KDoc link to a symbol that is excluded from the documentation (suppressed, `internal`,
 * deprecated when `skipDeprecated` is on, etc.) resolves to a valid `DRI` during analysis,
 * but the symbol has no page. Such a link must not be rendered as a working `<a>` link to a
 * non-existent page, and Dokka should warn the developer about it (#4448).
 */
class LinkToExcludedSymbolTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    private val source = """
        |/src/main/kotlin/test/Test.kt
        |package test
        |
        |internal class Hidden
        |
        |/**
        | * See [Hidden] for details.
        | */
        |public class Public
        """.trimMargin()

    @Test
    fun `link to excluded symbol is rendered as unresolved span and warns`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(source, configuration, pluginOverrides = listOf(writerPlugin)) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.contents.getValue("root/test/-public/index.html")
                assertTrue(
                    content.contains("""data-unresolved-link="test/Hidden///PointingToDeclaration/""""),
                    "Expected an unresolved-link span for the excluded symbol, but got:\n$content"
                )
                assertFalse(
                    content.contains(Regex("""<a [^>]*href="[^"]*Hidden""")),
                    "A link to an excluded symbol must not be rendered as a working <a> link:\n$content"
                )
                assertTrue(
                    logger.warnMessages.any { it.contains("test/Hidden") && it.contains("Couldn't resolve link") },
                    "Expected a warning about the unresolved link, but got: ${logger.warnMessages}"
                )
            }
        }
    }

    @Test
    fun `link to excluded symbol fails the build when failOnWarning is enabled`() {
        val failOnWarningConfiguration = dokkaConfiguration {
            failOnWarning = true
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        assertFailsWith<DokkaException> {
            testInline(source, failOnWarningConfiguration) {}
        }
    }
}
