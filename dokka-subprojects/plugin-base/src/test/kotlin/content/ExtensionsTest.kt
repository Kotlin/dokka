/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package content

import matchers.content.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionsTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    // function tests

    @Test
    fun `should render top-level extension function`() {
        testInline(
            """
            /src/A.kt
            fun String.extension() {}
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single() as ContentPage

                pkgNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.EXTENSION_FUNCTION) {
                            assertTableWithTabs(
                                "extension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should differentiate top-level function and extension function`() {
        testInline(
            """
            /src/A.kt
            fun function() {}
            fun String.extension() {}
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single() as ContentPage

                pkgNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.FUNCTION) {
                            assertTableWithTabs(
                                "extension" to BasicTabbedContentType.EXTENSION_FUNCTION,
                                "function" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render member extension function for object`() {
        testInline(
            """
            /src/A.kt
            object A {
                fun String.memberExtension() {}  
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.FUNCTION) {
                            assertTableWithTabs(
                                "memberExtension" to null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render top-level extension function for object`() {
        testInline(
            """
            /src/A.kt
            object A
            fun A.topLevelExtension() {}
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.EXTENSION_FUNCTION) {
                            assertTableWithTabs(
                                "topLevelExtension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should differentiate functions and extension functions for object`() {
        testInline(
            """
            /src/A.kt
            object A {
                fun function() {}
                fun String.memberExtension() {}  
            }
            fun A.extension() {}
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.FUNCTION) {
                            assertTableWithTabs(
                                "extension" to BasicTabbedContentType.EXTENSION_FUNCTION,
                                "function" to null,
                                "memberExtension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render member extensions function for class`() {
        testInline(
            """
            /src/A.kt
            class A {
                fun String.memberExtension() {}  
                fun A.memberSelfExtension() {}  
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.FUNCTION) {
                            assertTableWithTabs(
                                "memberExtension" to null,
                                "memberSelfExtension" to null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render top-level extension functions for class`() {
        testInline(
            """
            /src/A.kt
            class A  
            fun A.topLevelExtension() {}  
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.EXTENSION_FUNCTION) {
                            assertTableWithTabs(
                                "topLevelExtension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render extension functions from object for class`() {
        testInline(
            """
            /src/A.kt
            class A
            object B {
                fun A.extensionFromB() {}
            } 
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.EXTENSION_FUNCTION) {
                            assertTableWithTabs(
                                "extensionFromB" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should differentiate functions and extension functions for class`() {
        testInline(
            """
            /src/A.kt
            class A {
                fun function() {}
                fun String.memberExtension() {}  
                fun A.memberSelfExtension() {}  
            }
            fun A.extension() {}
            object B {
                fun A.extensionFromB() {}
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.FUNCTION) {
                            assertTableWithTabs(
                                "extension" to BasicTabbedContentType.EXTENSION_FUNCTION,
                                "extensionFromB" to BasicTabbedContentType.EXTENSION_FUNCTION,
                                "function" to null,
                                "memberExtension" to null,
                                "memberSelfExtension" to null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render member extension functions for companion object of class`() {
        testInline(
            """
            /src/A.kt
            class A {
                companion object {
                    fun Int.companionMemberExtensionForInt() {}
                    fun A.companionMemberExtensionForA() {}
                }
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Types", BasicTabbedContentType.TYPE) { skipAllNotMatching() }
                        assertTabGroup("Functions", BasicTabbedContentType.EXTENSION_FUNCTION) {
                            assertTableWithTabs(
                                "companionMemberExtensionForA" to null,
                            )
                        }
                    }
                }

                val companionPage = objectNode.children.single { it.name == "Companion" } as ContentPage

                companionPage.content.assertTabbedNode {
                    group {
                        assertTabGroup("Functions", BasicTabbedContentType.FUNCTION) {
                            assertTableWithTabs(
                                "companionMemberExtensionForA" to null,
                                "companionMemberExtensionForInt" to null
                            )
                        }
                    }
                }
            }
        }
    }

    // property tests

    @Test
    fun `should render top-level extension property`() {
        testInline(
            """
            /src/A.kt
            val String.extension: String get() = "" 
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single() as ContentPage

                pkgNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.EXTENSION_PROPERTY) {
                            assertTableWithTabs(
                                "extension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should differentiate top-level property and extension property`() {
        testInline(
            """
            /src/A.kt
            val property: String get() = ""
            val String.extension: String get() = ""
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single() as ContentPage

                pkgNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.PROPERTY) {
                            assertTableWithTabs(
                                "extension" to BasicTabbedContentType.EXTENSION_PROPERTY,
                                "property" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render member extension property for object`() {
        testInline(
            """
            /src/A.kt
            object A {
                val String.memberExtension: String get() = ""
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.PROPERTY) {
                            assertTableWithTabs(
                                "memberExtension" to null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render top-level extension property for object`() {
        testInline(
            """
            /src/A.kt
            object A
            val A.topLevelExtension: String get() = ""
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.EXTENSION_PROPERTY) {
                            assertTableWithTabs(
                                "topLevelExtension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should differentiate properties and extension properties for object`() {
        testInline(
            """
            /src/A.kt
            object A {
                val property: String get() = ""
                val String.memberExtension: String get() = ""
            }
            val A.extension: String get() = ""
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.PROPERTY) {
                            assertTableWithTabs(
                                "extension" to BasicTabbedContentType.EXTENSION_PROPERTY,
                                "memberExtension" to null,
                                "property" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render member extension properties for class`() {
        testInline(
            """
            /src/A.kt
            class A {
                val String.memberExtension: String get() = ""
                val A.memberSelfExtension: String get() = ""
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.PROPERTY) {
                            assertTableWithTabs(
                                "memberExtension" to null,
                                "memberSelfExtension" to null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render top-level extension properties for class`() {
        testInline(
            """
            /src/A.kt
            class A  
            val A.topLevelExtension: String get() = ""  
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.EXTENSION_PROPERTY) {
                            assertTableWithTabs(
                                "topLevelExtension" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render extension properties from object for class`() {
        testInline(
            """
            /src/A.kt
            class A
            object B {
                val A.extensionFromB: String get() = ""
            } 
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.EXTENSION_PROPERTY) {
                            assertTableWithTabs(
                                "extensionFromB" to null
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should differentiate properties and extension properties for class`() {
        testInline(
            """
            /src/A.kt
            class A {
                val property: String get() = ""
                val String.memberExtension: String get() = ""  
                val A.memberSelfExtension: String get() = ""  
            }
            val A.extension: String get() = ""
            object B {
                val A.extensionFromB: String get() = ""
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.PROPERTY) {
                            assertTableWithTabs(
                                "extension" to BasicTabbedContentType.EXTENSION_PROPERTY,
                                "extensionFromB" to BasicTabbedContentType.EXTENSION_PROPERTY,
                                "memberExtension" to null,
                                "memberSelfExtension" to null,
                                "property" to null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `should render member extension properties for companion object of class`() {
        testInline(
            """
            /src/A.kt
            class A {
                companion object {
                    val Int.companionMemberExtensionForInt: String get() = ""
                    val A.companionMemberExtensionForA: String get() = ""
                }
            }
            """.trimIndent(),
            configuration
        ) {
            pagesGenerationStage = { modulePage ->
                val pkgNode = modulePage.children.single()
                val objectNode = pkgNode.children.single { it.name == "A" } as ContentPage

                objectNode.content.assertTabbedNode {
                    group {
                        assertTabGroup("Constructors", BasicTabbedContentType.CONSTRUCTOR) { skipAllNotMatching() }
                    }
                    group {
                        assertTabGroup("Types", BasicTabbedContentType.TYPE) { skipAllNotMatching() }
                        assertTabGroup("Properties", BasicTabbedContentType.EXTENSION_PROPERTY) {
                            assertTableWithTabs(
                                "companionMemberExtensionForA" to null,
                            )
                        }
                    }
                }

                val companionPage = objectNode.children.single { it.name == "Companion" } as ContentPage

                companionPage.content.assertTabbedNode {
                    group {
                        assertTabGroup("Properties", BasicTabbedContentType.PROPERTY) {
                            assertTableWithTabs(
                                "companionMemberExtensionForA" to null,
                                "companionMemberExtensionForInt" to null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ContentMatcherBuilder<ContentGroup>.assertTableWithTabs(
        vararg expected: Pair<String, TabbedContentType?>
    ) {
        table {
            expected.forEach { (name, tabType) ->
                group {
                    assertTabbedContentType(tabType)
                    link { +name }
                    skipAllNotMatching()
                }
            }
        }
    }

    private fun ContentMatcherBuilder<ContentGroup>.assertTabbedContentType(expected: TabbedContentType?) {
        check {
            assertEquals(expected, extra[TabbedContentTypeExtra]?.value)
        }
    }

    private fun ContentNode.assertTabbedNode(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
        assertNode {
            group {
                header { }
                skipAllNotMatching()
            }
            tabbedGroup(block)
        }
    }

    private fun ContentMatcherBuilder<ContentGroup>.assertTabGroup(
        name: String,
        type: TabbedContentType,
        block: ContentMatcherBuilder<ContentGroup>.() -> Unit
    ) {
        group {
            assertTabbedContentType(type)
            header { +name }
            block()
        }
    }

}
