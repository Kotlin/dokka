/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package renderers.html

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.TestOutputWriterPlugin
import utils.navigationHtml
import utils.selectTocLinkGrid
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationIconTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `should include all icons`() {
        val source = """
            |/src/main/kotlin/com/example/Empty.kt
            |package com.example
            |
            |class Empty {}
            """
        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val navIconAssets = writerPlugin.writer.contents
                    .filterKeys { it.startsWith("images") }
                    .keys.sorted()

                assertEquals(31, navIconAssets.size)
                assertEquals("images/abstract-class-kotlin.svg", navIconAssets[0])
                assertEquals("images/abstract-class.svg", navIconAssets[1])
                assertEquals("images/anchor-copy-icon.svg", navIconAssets[2])
                assertEquals("images/annotation-kotlin.svg", navIconAssets[3])
                assertEquals("images/annotation.svg", navIconAssets[4])
                assertEquals("images/arrow-down.svg", navIconAssets[5])
                assertEquals("images/burger.svg", navIconAssets[6])
                assertEquals("images/check.svg", navIconAssets[7])
                assertEquals("images/checkbox-off.svg", navIconAssets[8])
                assertEquals("images/checkbox-on.svg", navIconAssets[9])
                assertEquals("images/class-kotlin.svg", navIconAssets[10])
                assertEquals("images/class.svg", navIconAssets[11])
                assertEquals("images/copy-icon.svg", navIconAssets[12])
                assertEquals("images/cross.svg", navIconAssets[13])
                assertEquals("images/enum-kotlin.svg", navIconAssets[14])
                assertEquals("images/enum.svg", navIconAssets[15])
                assertEquals("images/exception-class.svg", navIconAssets[16])
                assertEquals("images/field-value.svg", navIconAssets[17])
                assertEquals("images/field-variable.svg", navIconAssets[18])
                assertEquals("images/filter.svg", navIconAssets[19])
                assertEquals("images/function.svg", navIconAssets[20])
                assertEquals("images/go-to-top-icon.svg", navIconAssets[21])
                assertEquals("images/homepage.svg", navIconAssets[22])
                assertEquals("images/interface-kotlin.svg", navIconAssets[23])
                assertEquals("images/interface.svg", navIconAssets[24])
                assertEquals("images/logo-icon.svg", navIconAssets[25])
                assertEquals("images/object.svg", navIconAssets[26])
                assertEquals("images/placeholder.svg", navIconAssets[27])
                assertEquals("images/success-icon.svg", navIconAssets[28])
                assertEquals("images/theme-toggle.svg", navIconAssets[29])
                assertEquals("images/typealias-kotlin.svg", navIconAssets[30])
            }
        }
    }

    @Test
    fun `should add icon styles to kotlin class navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("class Clazz {}"),
            expectedIconClass = "class-kt",
            expectedNavLinkText = "Clazz"
        )
    }

    @Test
    fun `should add icon styles to java class navigation item`() {
        assertNavigationIcon(
            source = javaSource(
                className = "JavaClazz",
                source = "public class JavaClazz {}"
            ),
            expectedIconClass = "class",
            expectedNavLinkText = "JavaClazz"
        )
    }

    @Test
    fun `should add icon styles to kotlin abstract class navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("abstract class AbstractClazz {}"),
            expectedIconClass = "abstract-class-kt",
            expectedNavLinkText = "AbstractClazz"
        )
    }

    @Test
    fun `should add icon styles to java abstract class navigation item`() {
        assertNavigationIcon(
            source = javaSource(
                className = "AbstractJavaClazz",
                source = "public abstract class AbstractJavaClazz {}"
            ),
            expectedIconClass = "abstract-class",
            expectedNavLinkText = "AbstractJavaClazz"
        )
    }

    @Test
    fun `should add icon styles to kotlin typealias navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("typealias KotlinTypealias = String"),
            expectedIconClass = "typealias-kt",
            expectedNavLinkText = "KotlinTypealias"
        )
    }

    @Test
    fun `should add icon styles to kotlin enum navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("enum class KotlinEnum {}"),
            expectedIconClass = "enum-class-kt",
            expectedNavLinkText = "KotlinEnum"
        )
    }

    @Test
    fun `should add icon styles to java enum class navigation item`() {
        assertNavigationIcon(
            source = javaSource(
                className = "JavaEnum",
                source = "public enum JavaEnum {}"
            ),
            expectedIconClass = "enum-class",
            expectedNavLinkText = "JavaEnum"
        )
    }

    @Test
    fun `should add icon styles to kotlin annotation navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("annotation class KotlinAnnotation"),
            expectedIconClass = "annotation-class-kt",
            expectedNavLinkText = "KotlinAnnotation"
        )
    }

    @Test
    fun `should add icon styles to java annotation navigation item`() {
        assertNavigationIcon(
            source = javaSource(
                className = "JavaAnnotation",
                source = "public @interface JavaAnnotation {}"
            ),
            expectedIconClass = "annotation-class",
            expectedNavLinkText = "JavaAnnotation"
        )
    }


    @Test
    fun `should add icon styles to kotlin interface navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("interface KotlinInterface"),
            expectedIconClass = "interface-kt",
            expectedNavLinkText = "KotlinInterface"
        )
    }

    @Test
    fun `should add icon styles to java interface navigation item`() {
        assertNavigationIcon(
            source = javaSource(
                className = "JavaInterface",
                source = "public interface JavaInterface {}"
            ),
            expectedIconClass = "interface",
            expectedNavLinkText = "JavaInterface"
        )
    }

    @Test
    fun `should add icon styles to kotlin function navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("fun ktFunction() {}"),
            expectedIconClass = "function",
            expectedNavLinkText = "ktFunction()"
        )
    }

    @Test
    fun `should add icon styles to kotlin exception class navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("class KotlinException : Exception() {}"),
            expectedIconClass = "exception-class",
            expectedNavLinkText = "KotlinException"
        )
    }

    @Test
    fun `should add icon styles to kotlin object navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("object KotlinObject {}"),
            expectedIconClass = "object",
            expectedNavLinkText = "KotlinObject"
        )
    }

    @Test
    fun `should add icon styles to kotlin val navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("val value: String? = null"),
            expectedIconClass = "val",
            expectedNavLinkText = "value"
        )
    }

    @Test
    fun `should add icon styles to kotlin var navigation item`() {
        assertNavigationIcon(
            source = kotlinSource("var variable: String? = null"),
            expectedIconClass = "var",
            expectedNavLinkText = "variable"
        )
    }

    private fun kotlinSource(source: String): String {
        return """
            |/src/main/kotlin/com/example/Example.kt
            |package com.example
            |
            |$source
            """.trimIndent()
    }

    private fun javaSource(className: String, source: String): String {
        return """
            |/src/main/java/com/example/$className.java
            |package com.example;
            |
            |$source
            """.trimIndent()
    }

    private fun assertNavigationIcon(source: String, expectedIconClass: String, expectedNavLinkText: String) {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select(".toc--part")
                val navigationGrid = content.selectTocLinkGrid()

                val classNames = navigationGrid.child(0).classNames().toList()
                assertEquals("toc--icon", classNames[0])
                assertEquals(expectedIconClass, classNames[1])

                val navLinkText = navigationGrid.child(1).text()
                assertEquals(expectedNavLinkText, navLinkText)
            }
        }
    }

    @Test
    fun `should not generate nav link grids or icons for packages and modules`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/com/example/Example.kt
            |package com.example
            |
            |class Example {}
            """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val content = writerPlugin.writer.navigationHtml().select(".toc--part")

                assertEquals(3, content.size)
                assertEquals("root-nav-submenu", content[0].id())
                assertEquals("root-nav-submenu-0", content[1].id())
                assertEquals("root-nav-submenu-0-0", content[2].id())

                // there's 3 nav items, but only one icon
                val navLinkGrids = content.select(".toc--icon")
                assertEquals(1, navLinkGrids.size)
            }
        }
    }
}
