/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import utils.TestOutputWriterPlugin
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.getValue
import kotlin.collections.single
import kotlin.let

abstract class AbstractRenderingTest : org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest() {
    val testDataDir: java.nio.file.Path = org.jetbrains.dokka.testApi.testRunner.AbstractTest.getTestDataDir("multiplatform/basicMultiplatformTest")
        .toAbsolutePath()

    val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.moduleName = "example"
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            val common = testApi.testRunner.SourceSetsBuilder.sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots =
                    kotlin.collections.listOf(java.nio.file.Paths.get("$testDataDir/commonMain/kotlin").toString())
            }
            val jvmAndJsSecondCommonMain = testApi.testRunner.SourceSetsBuilder.sourceSet {
                name = "jvmAndJsSecondCommonMain"
                displayName = "jvmAndJsSecondCommonMain"
                analysisPlatform = "common"
                dependentSourceSets = kotlin.collections.setOf(common.value.sourceSetID)
                sourceRoots = kotlin.collections.listOf(
                    java.nio.file.Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString()
                )
            }
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                name = "js"
                displayName = "js"
                analysisPlatform = "js"
                dependentSourceSets =
                    kotlin.collections.setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                sourceRoots =
                    kotlin.collections.listOf(java.nio.file.Paths.get("$testDataDir/jsMain/kotlin").toString())
            }
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets =
                    kotlin.collections.setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                sourceRoots =
                    kotlin.collections.listOf(java.nio.file.Paths.get("$testDataDir/jvmMain/kotlin").toString())
            }
        }
    }

    fun utils.TestOutputWriterPlugin.renderedContent(path: String): org.jsoup.nodes.Element = utils.TestOutputWriterPlugin.writer.contents.getValue(path)
        .let { org.jsoup.Jsoup.parse(it) }.select("#content").single()

    fun utils.TestOutputWriterPlugin.renderedDivergentContent(path: String): org.jsoup.select.Elements =
        renderedContent(path).select("div.divergent-group")

    fun utils.TestOutputWriterPlugin.renderedSourceDependentContent(path: String): org.jsoup.select.Elements =
        renderedContent(path).select("div.sourceset-dependent-content")

    val org.jsoup.nodes.Element.brief: String
        get() = org.jsoup.nodes.Element.children().select("p").text()

    val org.jsoup.nodes.Element.rawBrief: String
        get() = org.jsoup.nodes.Element.children().select("p").html()
}
