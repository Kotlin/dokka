package renderers.html

import org.jetbrains.dokka.base.renderers.html.NavigationNodeIcon
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import kotlin.test.assertEquals
import utils.navigationHtml
import kotlin.test.assertNull

class NavigationTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `should sort alphabetically ignoring case`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/com/example/Sequences.kt
            |package com.example
            |
            |fun <T> sequence(): Sequence<T>
            |
            |fun <T> Sequence(): Sequence<T>
            |
            |fun <T> Sequence<T>.any() {}
            |
            |interface Sequence<T>
            """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select("div.sideMenuPart")
                assertEquals(6, content.size)

                // Navigation menu should be the following:
                // - root
                //    - com.example
                //       - any()
                //       - Sequence interface
                //       - Sequence()
                //       - sequence()

                content[0].assertNavigationLink(
                    id = "root-nav-submenu",
                    text = "root",
                    address = "index.html",
                )

                content[1].assertNavigationLink(
                    id = "root-nav-submenu-0",
                    text = "com.example",
                    address = "root/com.example/index.html",
                )

                content[2].assertNavigationLink(
                    id = "root-nav-submenu-0-0",
                    text = "any()",
                    address = "root/com.example/any.html",
                    icon = NavigationNodeIcon.FUNCTION
                )

                content[3].assertNavigationLink(
                    id = "root-nav-submenu-0-1",
                    text = "Sequence",
                    address = "root/com.example/-sequence/index.html",
                    icon = NavigationNodeIcon.INTERFACE_KT
                )

                content[4].assertNavigationLink(
                    id = "root-nav-submenu-0-2",
                    text = "Sequence()",
                    address = "root/com.example/-sequence.html",
                    icon = NavigationNodeIcon.FUNCTION
                )

                content[5].assertNavigationLink(
                    id = "root-nav-submenu-0-3",
                    text = "sequence()",
                    address = "root/com.example/sequence.html",
                    icon = NavigationNodeIcon.FUNCTION
                )
            }
        }
    }

    @Test
    fun `should strike deprecated class link`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/com/example/SimpleDeprecatedClass.kt
            |package com.example
            |
            |@Deprecated("reason")
            |class SimpleDeprecatedClass {}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select("div.sideMenuPart")
                assertEquals(3, content.size)

                // Navigation menu should be the following:
                // - root
                //    - com.example
                //       - SimpleDeprecatedClass

                content[0].assertNavigationLink(
                    id = "root-nav-submenu",
                    text = "root",
                    address = "index.html",
                )

                content[1].assertNavigationLink(
                    id = "root-nav-submenu-0",
                    text = "com.example",
                    address = "root/com.example/index.html",
                )

                content[2].assertNavigationLink(
                    id = "root-nav-submenu-0-0",
                    text = "SimpleDeprecatedClass",
                    address = "root/com.example/-simple-deprecated-class/index.html",
                    icon = NavigationNodeIcon.CLASS_KT,
                    isStrikethrough = true
                )
            }
        }
    }

    @Test
    fun `should not strike pages where only one of N documentables is deprecated`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/com/example/File.kt
            |package com.example
            |
            |/**
            | * First
            | */
            |@Deprecated("reason")
            |fun functionWithCommonName()
            |
            |/**
            | * Second
            | */
            |fun functionWithCommonName()
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select("div.sideMenuPart")
                assertEquals(3, content.size)

                // Navigation menu should be the following:
                // - root
                //    - com.example
                //       - functionWithCommonName

                content[0].assertNavigationLink(
                    id = "root-nav-submenu",
                    text = "root",
                    address = "index.html",
                )

                content[1].assertNavigationLink(
                    id = "root-nav-submenu-0",
                    text = "com.example",
                    address = "root/com.example/index.html",
                )

                content[2].assertNavigationLink(
                    id = "root-nav-submenu-0-0",
                    text = "functionWithCommonName()",
                    address = "root/com.example/function-with-common-name.html",
                    icon = NavigationNodeIcon.FUNCTION,
                    isStrikethrough = false
                )
            }
        }
    }

    @Test
    fun `should have expandable classlikes`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/com/example/WithInner.kt
            |package com.example
            |
            |class WithInner {
            |    // in-class functions should not be in navigation
            |    fun a() {}
            |    fun b() {}
            |    fun c() {}
            |
            |    class InnerClass {}
            |    interface InnerInterface {}
            |    enum class InnerEnum {}
            |    object InnerObject {}
            |    annotation class InnerAnnotation {}
            |    companion object CompanionObject {}
            |}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select("div.sideMenuPart")
                assertEquals(9, content.size)

                // Navigation menu should be the following, sorted by name:
                // - root
                //    - com.example
                //       - WithInner
                //          - CompanionObject
                //          - InnerAnnotation
                //          - InnerClass
                //          - InnerEnum
                //          - InnerInterface
                //          - InnerObject

                content[0].assertNavigationLink(
                    id = "root-nav-submenu",
                    text = "root",
                    address = "index.html",
                )

                content[1].assertNavigationLink(
                    id = "root-nav-submenu-0",
                    text = "com.example",
                    address = "root/com.example/index.html",
                )

                content[2].assertNavigationLink(
                    id = "root-nav-submenu-0-0",
                    text = "WithInner",
                    address = "root/com.example/-with-inner/index.html",
                    icon = NavigationNodeIcon.CLASS_KT
                )

                content[3].assertNavigationLink(
                    id = "root-nav-submenu-0-0-0",
                    text = "CompanionObject",
                    address = "root/com.example/-with-inner/-companion-object/index.html",
                    icon = NavigationNodeIcon.OBJECT
                )

                content[4].assertNavigationLink(
                    id = "root-nav-submenu-0-0-1",
                    text = "InnerAnnotation",
                    address = "root/com.example/-with-inner/-inner-annotation/index.html",
                    icon = NavigationNodeIcon.ANNOTATION_CLASS_KT
                )

                content[5].assertNavigationLink(
                    id = "root-nav-submenu-0-0-2",
                    text = "InnerClass",
                    address = "root/com.example/-with-inner/-inner-class/index.html",
                    icon = NavigationNodeIcon.CLASS_KT
                )

                content[6].assertNavigationLink(
                    id = "root-nav-submenu-0-0-3",
                    text = "InnerEnum",
                    address = "root/com.example/-with-inner/-inner-enum/index.html",
                    icon = NavigationNodeIcon.ENUM_CLASS_KT
                )

                content[7].assertNavigationLink(
                    id = "root-nav-submenu-0-0-4",
                    text = "InnerInterface",
                    address = "root/com.example/-with-inner/-inner-interface/index.html",
                    icon = NavigationNodeIcon.INTERFACE_KT
                )

                content[8].assertNavigationLink(
                    id = "root-nav-submenu-0-0-5",
                    text = "InnerObject",
                    address = "root/com.example/-with-inner/-inner-object/index.html",
                    icon = NavigationNodeIcon.OBJECT
                )
            }
        }
    }

    @Test
    fun `should be able to have deeply nested classlikes`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/com/example/DeeplyNested.kt
            |package com.example
            |
            |class DeeplyNested {
            |    class FirstLevelClass {
            |        interface SecondLevelInterface {
            |            object ThirdLevelObject {
            |                annotation class FourthLevelAnnotation {}
            |            }
            |        }
            |    }
            |}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select("div.sideMenuPart")
                assertEquals(7, content.size)

                // Navigation menu should be the following
                // - root
                //    - com.example
                //       - DeeplyNested
                //          - FirstLevelClass
                //             - SecondLevelInterface
                //                - ThirdLevelObject
                //                   - FourthLevelAnnotation

                content[0].assertNavigationLink(
                    id = "root-nav-submenu",
                    text = "root",
                    address = "index.html",
                )

                content[1].assertNavigationLink(
                    id = "root-nav-submenu-0",
                    text = "com.example",
                    address = "root/com.example/index.html",
                )

                content[2].assertNavigationLink(
                    id = "root-nav-submenu-0-0",
                    text = "DeeplyNested",
                    address = "root/com.example/-deeply-nested/index.html",
                    icon = NavigationNodeIcon.CLASS_KT
                )

                content[3].assertNavigationLink(
                    id = "root-nav-submenu-0-0-0",
                    text = "FirstLevelClass",
                    address = "root/com.example/-deeply-nested/-first-level-class/index.html",
                    icon = NavigationNodeIcon.CLASS_KT
                )

                content[4].assertNavigationLink(
                    id = "root-nav-submenu-0-0-0-0",
                    text = "SecondLevelInterface",
                    address = "root/com.example/-deeply-nested/-first-level-class/-second-level-interface/index.html",
                    icon = NavigationNodeIcon.INTERFACE_KT
                )

                content[5].assertNavigationLink(
                    id = "root-nav-submenu-0-0-0-0-0",
                    text = "ThirdLevelObject",
                    address = "root/com.example/-deeply-nested/-first-level-class/-second-level-interface/" +
                            "-third-level-object/index.html",
                    icon = NavigationNodeIcon.OBJECT
                )

                content[6].assertNavigationLink(
                    id = "root-nav-submenu-0-0-0-0-0-0",
                    text = "FourthLevelAnnotation",
                    address = "root/com.example/-deeply-nested/-first-level-class/-second-level-interface/" +
                            "-third-level-object/-fourth-level-annotation/index.html",
                    icon = NavigationNodeIcon.ANNOTATION_CLASS_KT
                )
            }
        }
    }

    private fun Element.assertNavigationLink(
        id: String, text: String, address: String, icon: NavigationNodeIcon? = null, isStrikethrough: Boolean = false
    ) {
        assertEquals(id, this.id())

        val link = this.selectFirst("a")
        checkNotNull(link)
        assertEquals(text, link.text())
        assertEquals(address, link.attr("href"))
        if (icon != null) {
            val iconStyles =
                this.selectFirst("div.overview span.nav-link-grid")?.child(0)?.classNames()?.toList() ?: emptyList()
            assertEquals(3, iconStyles.size)
            assertEquals("nav-link-child", iconStyles[0])
            assertEquals(icon.style(), "${iconStyles[1]} ${iconStyles[2]}")
        }
        if (isStrikethrough) {
            val textInsideStrikethrough = link.selectFirst("strike")?.text()
            assertEquals(text, textInsideStrikethrough)
        } else {
            assertNull(link.selectFirst("strike"))
        }
    }
}
