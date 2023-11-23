/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package enums

import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import signatures.renderedContent
import utils.TestOutputWriterPlugin
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaEnumsTest : BaseAbstractTest() {

    private val basicConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    // Shouldn't try to give source links to synthetic methods (values, valueOf) if any are present
    // https://github.com/Kotlin/dokka/issues/2544
    @Test
    fun `java enum with configured source links should not fail build due to any synthetic methods`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    sourceLinks = listOf(
                        SourceLinkDefinitionImpl(
                            localDirectory = "src/main/java",
                            remoteUrl = URL("https://github.com/user/repo/tree/master/src/main/java"),
                            remoteLineSuffix = "#L"
                        )
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/java/basic/JavaEnum.java
            |package testpackage
            |
            |/**
            |* doc
            |*/
            |public enum JavaEnum {
            |    ONE, TWO, THREE
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val enumPage = writerPlugin.writer.renderedContent("root/testpackage/-java-enum/index.html")
                val sourceLink = enumPage.select(".symbol .floating-right")
                    .select("a[href]")
                    .attr("href")


                assertEquals(
                    "https://github.com/user/repo/tree/master/src/main/java/basic/JavaEnum.java#L6",
                    sourceLink
                )
            }
        }
    }
}
