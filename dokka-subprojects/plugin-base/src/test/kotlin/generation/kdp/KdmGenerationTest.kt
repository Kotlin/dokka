/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package generation.kdp

import org.jetbrains.dokka.base.generation.kdp.saveModule
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class KdmGenerationTest : BaseAbstractTest() {

    /**
     * KDM output is kept in the repository (`kdm-renderer/samples/kdp`) so that changes
     * to the mapping are visible in the diff.
     */
    private val sampleOutputDir = File("../../kdm-renderer/samples").canonicalFile

    @Test
    fun `should produce kdm json for a sample module`() {
        val configuration = dokkaConfiguration {
            moduleName = "kdm-sample"
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/example")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/example/Sample.kt
            |package example
            |
            |/**
            | * A sample class.
            | *
            | * Second paragraph with `code`, **bold** and a [link][Container].
            | *
            | * @see Container.transform
            | */
            |public class Sample(public val answer: Int = 42) {
            |
            |    /**
            |     * Does something useful.
            |     *
            |     * @param input the input value
            |     * @return the formatted result
            |     * @throws IllegalArgumentException if [input] is negative
            |     */
            |    public fun doIt(input: Int): String = "${'$'}input"
            |
            |    /** A nested class. */
            |    public class Nested
            |
            |    public companion object {
            |        public const val DEFAULT: Int = 42
            |    }
            |}
            |
            |/**
            | * A generic container.
            | *
            | * @param T the element type
            | */
            |public interface Container<out T> {
            |    /** The stored value. */
            |    public val value: T
            |
            |    /** Transforms the value. */
            |    public fun <R> transform(block: (T) -> R): Container<R>
            |}
            |
            |/** Sample states. */
            |public enum class State {
            |    /** Not started yet. */
            |    IDLE,
            |
            |    /** In progress. */
            |    RUNNING
            |}
            |
            |/** A singleton. */
            |public object Registry {
            |    /** Registered names. */
            |    public var names: List<String> = emptyList()
            |}
            |
            |/** An annotation. */
            |@Target(AnnotationTarget.CLASS)
            |public annotation class Marker(public val name: String)
            |
            |/** A type alias. */
            |public typealias Names = List<String>
            |
            |/**
            | * An extension function.
            | *
            | * @receiver the string to check
            | * @return true if the string looks like a name
            | */
            |public fun String.isName(): Boolean = isNotBlank()
            |
            |/** A top-level property. */
            |public val version: String = "1.0"
            |
            |/** Deprecated on purpose. */
            |@Deprecated("Use isName() instead", ReplaceWith("isName()"))
            |public fun String.checkName(): Boolean = isName()
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { module ->
                sampleOutputDir.resolve("kdp").deleteRecursively()
                sampleOutputDir.mkdirs()

                saveModule(module, sampleOutputDir)

                val json = sampleOutputDir.resolve("kdp/kdm-sample-pretty.json")
                assertTrue(json.isFile, "expected KDM json at ${json.absolutePath}")

                val text = json.readText()
                assertContains(text, "\"Sample\"")
                assertContains(text, "\"doIt\"")
                assertContains(text, "A sample class")
                println("KDM written to ${json.parentFile.absolutePath}")
            }
        }
    }
}
