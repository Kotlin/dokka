/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content.seealso

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.ContentDRILink
import utils.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentForSeeAlsoTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOfNotNull(jvmStdlibPath)
                analysisPlatform = "jvm"
            }
        }
    }

    private val mppTestConfiguration = dokkaConfiguration {
        moduleName = "example"
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
            }
            sourceSet {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
            }
            sourceSet {
                name = "linuxX64"
                displayName = "linuxX64"
                analysisPlatform = "native"
                dependentSourceSets = setOf(common.value.sourceSetID)
                sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
            }
        }
    }

    @Test
    fun `undocumented function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented seealso`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented seealso without reference for class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc
            |  */
            |class Foo()
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Foo"
                            )
                            header(4) { +"See also" }
                            table {
                                group {
                                    +"abc"
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @OnlyDescriptors("No link for `abc` in K1")
    @Test
    fun `undocumented seealso with reference to parameter for class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc
            |  */
            |class Foo(abc: String)
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Foo",
                                "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                            )
                            header(4) { +"See also" }
                            table {
                                group {
                                    +"abc"  // link { +"abc" }
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @OnlyDescriptors("issue #3179")
    @Test
    fun `undocumented seealso with reference to property for class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc
            |  */
            |class Foo(val abc: String)
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Foo",
                                "val abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                            )
                            header(4) { +"See also" }
                            table {
                                group {
                                    link { +"Foo.abc" }
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `documented seealso`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                        group {
                                            group { +"Comment to abc" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OnlyDescriptors("issue #3179")
    @Test
    fun `documented seealso with reference to property for class`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc
            |  */
            |class Foo(val abc: String)
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "Foo")
                page.content.assertNode {
                    group {
                        header(1) { +"Foo" }
                        platformHinted {
                            classSignature(
                                emptyMap(),
                                "",
                                "",
                                emptySet(),
                                "Foo",
                                "val abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                            )
                            header(4) { +"See also" }
                            table {
                                group {
                                    link { +"Foo.abc" }
                                    group {
                                        group { +"Comment to abc" }
                                    }
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `should use fully qualified name for unresolved link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see com.example.NonExistingClass description for non-existing
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        +"com.example.NonExistingClass"
                                        group {
                                            group { +"description for non-existing" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented seealso with stdlib link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see Collection
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        link {
                                            check {
                                                assertEquals(
                                                    "kotlin.collections/Collection///PointingToDeclaration/",
                                                    (this as ContentDRILink).address.toString()
                                                )
                                            }
                                            +"Collection"
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented seealso with stdlib link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see Collection Comment to stdliblink
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"Collection" }
                                        group {
                                            group { +"Comment to stdliblink" }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented seealso with stdlib link with other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * random comment
            |  * @see Collection Comment to stdliblink
            |  * @author pikinier20
            |  * @since 0.11
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                group { comment { +"random comment" } }
                                unnamedTag("Author") { comment { +"pikinier20" } }
                                unnamedTag("Since") { comment { +"0.11" } }

                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"Collection" }
                                        group {
                                            group { +"Comment to stdliblink" }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented multiple see also`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc1
            |  * @see abc Comment to abc2
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                        group {
                                            group { +"Comment to abc2" }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented multiple see also mixed source`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc1
            |  * @see[Collection] Comment to collection
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("test", "function")
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"abc" }
                                        group {
                                            group { +"Comment to abc1" }
                                        }
                                    }
                                    group {
                                        //DRI should be "test//abc/#/-1/"
                                        link { +"Collection" }
                                        group { group { +"Comment to collection" } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should prefix static function and property links with class name`() {
        testInline(
            """
            |/src/main/kotlin/com/example/package/CollectionExtensions.kt
            |package com.example.util
            |
            |object CollectionExtensions {
            |    val property = "Hi"
            |    fun emptyList() {}
            |}
            |
            |/src/main/kotlin/com/example/foo.kt
            |package com.example
            |
            |import com.example.util.CollectionExtensions.property
            |import com.example.util.CollectionExtensions.emptyList
            |
            |/**
            | * @see [property] static property
            | * @see [emptyList] static emptyList
            | */
            |fun function() {}
            """.trimIndent(),
            testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("com.example", "function")

                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    annotations = emptyMap(),
                                    visibility = "",
                                    modifier = "",
                                    keywords = emptySet(),
                                    name = "function",
                                    returnType = null,
                                )
                            }
                            after {
                                header(4) { +"See also" }
                                table {
                                    group {
                                        link { +"CollectionExtensions.property" }
                                        group {
                                            group { +"static property" }
                                        }
                                    }
                                    group {
                                        link { +"CollectionExtensions.emptyList" }
                                        group {
                                            group { +"static emptyList" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `multiplatform class with seealso in few platforms`() {
        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/**
                |* @see Unit
                |*/
                |expect open class Parent
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |val x = 0
                |/**
                |* @see x resolved
                |* @see y unresolved
                |*/
                |actual open class Parent
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual open class Parent
                |
            """.trimMargin(),
            mppTestConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.findTestType("pageMerger", "Parent")
                page.content.assertNode {
                    group {
                        header(1) { +"Parent" }
                        platformHinted {
                            group {
                                +"expect open class "
                                link {
                                    +"Parent"
                                }
                            }
                            group {
                                +"actual open class "
                                link {
                                    +"Parent"
                                }
                            }
                            group {
                                +"actual open class "
                                link {
                                    +"Parent"
                                }
                            }
                            header(4) {
                                +"See also"
                                check {
                                    assertEquals(2, sourceSets.size)
                                }
                            }
                            table {
                                group {
                                    link { +"Unit" }
                                    check {
                                        sourceSets.assertSourceSet("common")
                                    }
                                }
                                group {
                                    link { +"Unit" }
                                    check {
                                        sourceSets.assertSourceSet("jvm")
                                    }
                                }
                                group {
                                    link { +"x" }
                                    group { group { +"resolved" } }
                                    check {
                                        sourceSets.assertSourceSet("jvm")
                                    }
                                }
                                group {
                                    +"y"
                                    group { group { +"unresolved" } }
                                    check {
                                        sourceSets.assertSourceSet("jvm")
                                    }
                                }

                                check {
                                    assertEquals(2, sourceSets.size)
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }
}

private fun Set<DisplaySourceSet>.assertSourceSet(expectedName: String) {
    assertEquals(1, this.size)
    assertEquals(expectedName, this.first().name)
}
