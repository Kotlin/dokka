/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentHeader
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import utils.OnlySymbols
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies that classlike pages expose two new sections defined by KEEP-0449:
 *   - "Companion functions"
 *   - "Companion properties"
 *
 * These sections collect every documentable carrying [org.jetbrains.dokka.model.CompanionBlockMember]:
 * Java static methods/fields, enum synthetic declarations (`values`/`valueOf`/`entries`),
 * Kotlin companion-block members, and companion extensions.
 *
 * They must be rendered between "Types" and "Properties"/"Functions".
 */
@OnlySymbols("companion block")
class CompanionSectionsTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    private fun ClasslikePageNode.findSectionWithName(name: String): ContentNode? {
        var sectionHeader: ContentHeader? = null
        return content.dfs { node ->
            node.children.filterIsInstance<ContentHeader>().any { header ->
                header.children.firstOrNull { it is ContentText && it.text == name }
                    ?.also { sectionHeader = header } != null
            }
        }?.children?.dropWhile { child -> child != sectionHeader }?.drop(1)?.firstOrNull()
    }

    private fun ClasslikePageNode.sectionTexts(name: String): List<String>? =
        findSectionWithName(name)?.children?.mapNotNull {
            (it.dfs { node -> node is ContentText } as? ContentText)?.text
        }

    private fun ClasslikePageNode.headerOrder(): List<String> {
        val seen = mutableListOf<String>()
        content.dfs { node ->
            for (h in node.children.filterIsInstance<ContentHeader>()) {
                val text = h.children.firstOrNull { it is ContentText }?.let { (it as ContentText).text }
                if (text != null && text !in seen) seen += text
            }
            false
        }
        return seen
    }

    @Test
    fun `java static method appears under Companion functions section`() {
        testInline(
            """
            |/src/example/Util.java
            |package example;
            |public class Util {
            |  public static int doStuff() { return 0; }
            |  public int instanceFn() { return 1; }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val util = root.dfs { it.name == "Util" } as? ClasslikePageNode
                    ?: error("Util page not found")

                val companionFns = util.sectionTexts("Companion functions")
                assertNotNull(companionFns, "Companion functions section must exist")
                assertEquals(listOf("doStuff"), companionFns, "expected 'doStuff' under Companion functions, got: $companionFns")

                val regularFns = util.sectionTexts("Functions").orEmpty()
                assertEquals(listOf("instanceFn"), regularFns, "instance method must remain in Functions, got: $regularFns")
            }
        }
    }

    @Test
    fun `java static field appears under Companion properties section`() {
        testInline(
            """
            |/src/example/Util.java
            |package example;
            |public class Util {
            |  public static final String NAME = "x";
            |  public int instanceField;
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val util = root.dfs { it.name == "Util" } as? ClasslikePageNode
                    ?: error("Util page not found")

                val companionProps = util.sectionTexts("Companion properties")
                assertNotNull(companionProps, "Companion properties section must exist")
                assertEquals(listOf("NAME"), companionProps, "expected 'NAME' under Companion properties, got: $companionProps")

                val regularProps = util.sectionTexts("Properties").orEmpty()
                assertEquals(listOf("instanceField"), regularProps, "instance field must remain in Properties, got: $regularProps")
            }
        }
    }

    @Test
    fun `enum synthetic declarations appear under Companion functions section`() {
        testInline(
            """
            |/src/main/kotlin/example/E.kt
            |package example
            |enum class E { A, B }
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val enumPage = root.dfs { it.name == "E" } as? ClasslikePageNode
                    ?: error("E page not found")
                val companionFns = enumPage.sectionTexts("Companion functions")
                    ?: error("Companion functions section must exist on enum page")

                assertEquals(listOf("valueOf", "values"), companionFns, "expected synthetic 'valueOf' and 'values' under Companion functions, got: $companionFns")

                val regularFns = enumPage.sectionTexts("Functions").orEmpty()
                assertEquals(emptyList(),regularFns, "no regular function is expected, got: $regularFns " )
            }
        }
    }

    @Test
    fun `Companion functions section appears after Types section`() {
        testInline(
            """
            |/src/example/Util.java
            |package example;
            |public class Util {
            |  public static class Nested {}
            |  public static int doStuff() { return 0; }
            |  public static final String NAME = "x";
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val util = root.dfs { it.name == "Util" } as? ClasslikePageNode
                    ?: error("Util page not found")
                val order = util.headerOrder()

                // The expected ordering puts companion sections right after Types and before
                // the regular Functions/Properties sections.
                val types = order.indexOf("Types")
                val companionProps = order.indexOf("Companion properties")
                val companionFns = order.indexOf("Companion functions")

                assertTrue(types >= 0, "Types section must exist, headers were: $order")
                assertTrue(companionProps >= 0, "Companion properties section must exist, headers were: $order")
                assertTrue(companionFns >= 0, "Companion functions section must exist, headers were: $order")

                assertTrue(types < companionProps, "Types must come before Companion properties, headers were: $order")
                assertTrue(types < companionFns, "Types must come before Companion functions, headers were: $order")
            }
        }
    }

    @Test
    fun `class without static members has no companion sections`() {
        testInline(
            """
            |/src/main/kotlin/example/Plain.kt
            |package example
            |class Plain {
            |    fun foo() {}
            |    val bar: Int = 0
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val plain = root.dfs { it.name == "Plain" } as? ClasslikePageNode
                    ?: error("Plain page not found")
                assertNull(plain.findSectionWithName("Companion functions"),
                    "no Companion functions section should be present")
                assertNull(plain.findSectionWithName("Companion properties"),
                    "no Companion properties section should be present")
            }
        }
    }

    @Test
    fun `companion object members do not move into Companion functions section`() {
        testInline(
            """
            |/src/main/kotlin/example/Holder.kt
            |package example
            |class Holder {
            |    companion object {
            |        fun create(): Holder = Holder()
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val holder = root.dfs { it.name == "Holder" && it is ClasslikePageNode } as? ClasslikePageNode
                    ?: error("Holder page not found")
                // Holder's own page must not have any Companion functions section coming
                // from its companion object — companion-object members live on the Companion
                // page, not in the enclosing class's static scope.
                assertNull(holder.findSectionWithName("Companion functions"),
                    "Companion functions section must not be present on Holder for companion-object members")
            }
        }
    }

    // ----------------------------------------------------------------------
    // KEEP-0449 Kotlin companion blocks: members declared inside a
    // `companion { ... }` block on a class. The Kotlin source has no instance
    // receiver for them, and Dokka collects them in the same companion-block
    // scope as Java statics and enum synthetic declarations.
    // ----------------------------------------------------------------------

    @Test
    fun `kotlin companion-block function appears under Companion functions section`() {
        testInline(
            """
            |/src/main/kotlin/example/Vector.kt
            |package example
            |class Vector(val x: Double, val y: Double) {
            |    fun length(): Double = 0.0
            |    companion {
            |        fun unit(): Vector = Vector(1.0, 1.0)
            |        fun zero(): Vector = Vector(0.0, 0.0)
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")

                val companionFns = page.sectionTexts("Companion functions")
                assertNotNull(companionFns, "Companion functions section must exist")
                assertEquals(listOf("unit", "zero"), companionFns,
                    "companion-block fun 'unit' must appear under Companion functions, got: $companionFns")

                val regularFns = page.sectionTexts("Functions").orEmpty()
                assertEquals(listOf("length"), regularFns,
                    "instance fun 'length' must remain in Functions, got: $regularFns")
            }
        }
    }

    @Test
    fun `kotlin companion-block property appears under Companion properties section`() {
        testInline(
            """
            |/src/main/kotlin/example/Vector.kt
            |package example
            |class Vector(val x: Double, val y: Double) {
            |    val name: String = "v"
            |    companion {
            |        val Zero: Vector = Vector(0.0, 0.0)
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")

                val companionProps = page.sectionTexts("Companion properties")
                assertNotNull(companionProps, "Companion properties section must exist")
                assertEquals(listOf("Zero"), companionProps,
                    "companion-block val 'Zero' must appear under Companion properties, got: $companionProps")

                val regularProps = page.sectionTexts("Properties").orEmpty()
                assertEquals(listOf("name", "x", "y"), regularProps,
                    "instance val 'name' must remain in Properties, got: $regularProps")
            }
        }
    }

    @Test
    fun `kotlin companion-block const val appears under Companion properties section`() {
        testInline(
            """
            |/src/main/kotlin/example/Vector.kt
            |package example
            |class Vector(val x: Double, val y: Double) {
            |    companion {
            |        const val Dimensions: Int = 2
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")
                val companionProps = page.sectionTexts("Companion properties")
                    ?: error("Companion properties section must exist")
                assertTrue("Dimensions" in companionProps,
                    "const val 'Dimensions' must appear under Companion properties, got: $companionProps")
            }
        }
    }

    @Test
    fun `kotlin companion-block sections appear after Types section`() {
        testInline(
            """
            |/src/main/kotlin/example/Vector.kt
            |package example
            |class Vector(val x: Double, val y: Double) {
            |    class Nested()
            |
            |    companion {
            |        fun unit(): Vector = Vector(1.0, 1.0)
            |        val Zero: Vector = Vector(0.0, 0.0)
            |    }
            |}
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")
                val order = page.headerOrder()

                val types = order.indexOf("Types")
                val companionProps = order.indexOf("Companion properties")
                val companionFns = order.indexOf("Companion functions")

                assertTrue(types >= 0, "Types section must exist, headers: $order")
                assertTrue(companionProps >= 0, "Companion properties section must exist, headers: $order")
                assertTrue(companionFns >= 0, "Companion functions section must exist, headers: $order")
                assertTrue(types < companionProps,
                    "Types must come before Companion properties, headers: $order")
                assertTrue(types < companionFns,
                    "Types must come before Companion functions, headers: $order")
            }
        }
    }

    @Test
    fun `kotlin companion extension appears under receiver's Companion functions section`() {
        testInline(
            """
            |/src/main/kotlin/example/Util.kt
            |package example
            |class Vector(val x: Double, val y: Double)
            |
            |companion fun Vector.unit(): Vector = Vector(1.0, 1.0)
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")
                val companionFns = page.sectionTexts("Companion functions")
                    ?: error("Companion functions section must exist on Vector")
                assertEquals(listOf("unit"), companionFns,
                    "companion extension 'unit' must appear under Vector's Companion functions, got: $companionFns")

                val regularFns = page.sectionTexts("Functions").orEmpty()
                assertTrue("unit" !in regularFns,
                    "companion extension 'unit' must NOT appear in regular Functions, got: $regularFns")
            }
        }
    }

    @Test
    fun `kotlin companion extension property appears under receiver's Companion properties section`() {
        testInline(
            """
            |/src/main/kotlin/example/Util.kt
            |package example
            |class Vector(val x: Double, val y: Double)
            |
            |companion val Vector.UnitX: Vector get() = Vector(1.0, 0.0)
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")
                val companionProps = page.sectionTexts("Companion properties")
                    ?: error("Companion properties section must exist on Vector")
                assertTrue("UnitX" in companionProps,
                    "companion extension property 'UnitX' must appear under Vector's Companion properties, got: $companionProps")

                val regularProps = page.sectionTexts("Properties").orEmpty()
                assertTrue("UnitX" !in regularProps,
                    "companion extension 'UnitX' must NOT appear in regular Properties, got: $regularProps")
            }
        }
    }

    @Test
    fun `plain top-level extension stays in regular Functions section`() {
        testInline(
            """
            |/src/main/kotlin/example/Util.kt
            |package example
            |class Vector(val x: Double, val y: Double)
            |
            |fun Vector.length(): Double = 0.0
            """.trimIndent(),
            configuration
        ) {
            pagesTransformationStage = { root ->
                val page = root.dfs { it.name == "Vector" } as? ClasslikePageNode
                    ?: error("Vector page not found")
                val regularFns = page.sectionTexts("Functions").orEmpty()
                assertTrue("length" in regularFns,
                    "plain extension 'length' must remain in Functions, got: $regularFns")
                assertNull(page.findSectionWithName("Companion functions"),
                    "plain extensions must not create a Companion functions section")
            }
        }
    }
}
