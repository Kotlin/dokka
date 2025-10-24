/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.runnablesamples

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RunnableSamplesTransformerTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `1 sample test`() {
        testInline(
            """
            |/src/main/kotlin/Sample.kt
            |package com.example
            |
            |fun sampleFunction() {
            |    println("This is a sample")
            |}
            |
            | /**
            | * @sample [com.example.sampleFunction]
            | */
            |class Foo
            """.trimMargin(),
            configuration = configuration
        ) {
            pagesTransformationStage = { rootPageNode ->
                val runnableSamples = findRunnableSamples(rootPageNode)
                assertEquals(1, runnableSamples.size)
                val runnableSample = runnableSamples[0]
                assertEquals("kotlin", runnableSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), runnableSample.style)
                val child = runnableSample.children[0]
                if (child is ContentText) {
                    assertEquals(
                        """
                        
                        fun main() { 
                           //sampleStart 
                           println("This is a sample") 
                           //sampleEnd
                        }
                    """.trimIndent(), child.text
                    )
                }
            }
        }
    }

    @Test
    fun `2 samples test`() {
        testInline(
            """
            |/src/main/kotlin/Sample.kt
            |package com.example
            |
            |fun sampleFunction1() {
            |    println("This is sample 1")
            |    val x = 42
            |    println("Value: ${'$'}x")
            |}
            |
            |fun sampleFunction2() {
            |    println("This is sample 2")
            |    val list = listOf(1, 2, 3)
            |    list.forEach { println(it) }
            |}
            |
            | /**
            | * @sample [com.example.sampleFunction1]
            | */
            |class Foo1
            |
            | /**
            | * @sample [com.example.sampleFunction2]
            | */
            |class Foo2
            """.trimMargin(),
            configuration = configuration
        ) {
            pagesTransformationStage = { rootPageNode ->
                val runnableSamples = findRunnableSamples(rootPageNode)
                assertEquals(2, runnableSamples.size)

                // Verify first sample
                val firstSample = runnableSamples[0]
                assertEquals("kotlin", firstSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), firstSample.style)
                val firstChild = firstSample.children[0]
                if (firstChild is ContentText) {
                    assertEquals(
                        """
                        
                        fun main() { 
                           //sampleStart 
                           println("This is sample 1")
                        val x = 42
                        println("Value: ${'$'}x") 
                           //sampleEnd
                        }
                    """.trimIndent(), firstChild.text
                    )
                }

                // Verify second sample
                val secondSample = runnableSamples[1]
                assertEquals("kotlin", secondSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), secondSample.style)
                val secondChild = secondSample.children[0]
                if (secondChild is ContentText) {
                    assertEquals(
                        """
                        
                        fun main() { 
                           //sampleStart 
                           println("This is sample 2")
                        val list = listOf(1, 2, 3)
                        list.forEach { println(it) } 
                           //sampleEnd
                        }
                    """.trimIndent(), secondChild.text
                    )
                }
            }
        }
    }

    @Test
    fun `3 samples test`() {
        testInline(
            """
            |/src/main/kotlin/Sample.kt
            |package com.example
            |
            |fun sampleFunction1() {
            |    println("First sample function")
            |    val greeting = "Hello"
            |    println("${'$'}greeting, World!")
            |}
            |
            |fun sampleFunction2() {
            |    println("Second sample function")
            |    val numbers = (1..5).toList()
            |    println("Numbers: ${'$'}numbers")
            |}
            |
            |fun sampleFunction3() {
            |    println("Third sample function")
            |    data class Person(val name: String, val age: Int)
            |    val person = Person("Alice", 30)
            |    println("Person: ${'$'}person")
            |}
            |
            | /**
            | * @sample [com.example.sampleFunction1]
            | */
            |class Foo1
            |
            | /**
            | * @sample [com.example.sampleFunction2] 
            | */
            |class Foo2
            |
            | /**
            | * @sample [com.example.sampleFunction3]
            | */
            |class Foo3
            """.trimMargin(),
            configuration = configuration
        ) {
            pagesTransformationStage = { rootPageNode ->
                val runnableSamples = findRunnableSamples(rootPageNode)
                assertEquals(3, runnableSamples.size)

                // Verify first sample
                val firstSample = runnableSamples[0]
                assertEquals("kotlin", firstSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), firstSample.style)
                val firstChild = firstSample.children[0]
                if (firstChild is ContentText) {
                    assertEquals(
                        """
                        
                        fun main() { 
                           //sampleStart 
                           println("First sample function")
                        val greeting = "Hello"
                        println("${'$'}greeting, World!") 
                           //sampleEnd
                        }
                    """.trimIndent(), firstChild.text
                    )
                }

                // Verify second sample
                val secondSample = runnableSamples[1]
                assertEquals("kotlin", secondSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), secondSample.style)
                val secondChild = secondSample.children[0]
                if (secondChild is ContentText) {
                    assertEquals(
                        """
                        
                        fun main() { 
                           //sampleStart 
                           println("Second sample function")
                        val numbers = (1..5).toList()
                        println("Numbers: ${'$'}numbers") 
                           //sampleEnd
                        }
                    """.trimIndent(), secondChild.text
                    )
                }

                // Verify third sample
                val thirdSample = runnableSamples[2]
                assertEquals("kotlin", thirdSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), thirdSample.style)
                val thirdChild = thirdSample.children[0]
                if (thirdChild is ContentText) {
                    assertEquals(
                        """
                        
                        fun main() { 
                           //sampleStart 
                           println("Third sample function")
                        data class Person(val name: String, val age: Int)
                        val person = Person("Alice", 30)
                        println("Person: ${'$'}person") 
                           //sampleEnd
                        }
                    """.trimIndent(), thirdChild.text
                    )
                }
            }
        }
    }

    @Test
    fun `sample with imports`() {
        testInline(
            """
            |/src/main/kotlin/Sample.kt
            |package com.example
            |
            |import java.util.Date
            |import kotlin.random.Random
            |
            |fun sampleFunctionWithImports() {
            |    val currentDate = Date()
            |    val randomNumber = Random.nextInt(1, 100)
            |    println("Current date: ${'$'}currentDate")
            |    println("Random number: ${'$'}randomNumber")
            |}
            |
            | /**
            | * @sample [com.example.sampleFunctionWithImports]
            | */
            |class FooWithImports
            """.trimMargin(),
            configuration = configuration
        ) {
            pagesTransformationStage = { rootPageNode ->
                val runnableSamples = findRunnableSamples(rootPageNode)
                assertEquals(1, runnableSamples.size)

                val runnableSample = runnableSamples[0]
                assertEquals("kotlin", runnableSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), runnableSample.style)

                val child = runnableSample.children[0]
                if (child is ContentText) {
                    assertEquals(
                        """
                        import java.util.Date
                        import kotlin.random.Random
                        
                        fun main() { 
                           //sampleStart 
                           val currentDate = Date()
                        val randomNumber = Random.nextInt(1, 100)
                        println("Current date: ${'$'}currentDate")
                        println("Random number: ${'$'}randomNumber") 
                           //sampleEnd
                        }
                    """.trimIndent(), child.text
                    )
                }
            }
        }
    }

    private fun findRunnableSamples(node: PageNode): List<ContentCodeBlock> {
        val samples = mutableListOf<ContentCodeBlock>()

        when (node) {
            is ContentPage -> {
                samples.addAll(findRunnableSamplesInContent(node.content))
            }
        }

        node.children.forEach { child ->
            samples.addAll(findRunnableSamples(child))
        }

        return samples
    }

    private fun findRunnableSamplesInContent(content: ContentNode): List<ContentCodeBlock> {
        val samples = mutableListOf<ContentCodeBlock>()

        when (content) {
            is ContentCodeBlock -> {
                if (content.style.contains(ContentStyle.RunnableSample)) {
                    samples.add(content)
                }
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentHeader -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentDivergentGroup -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child.divergent))
                    child.before?.let { samples.addAll(findRunnableSamplesInContent(it)) }
                    child.after?.let { samples.addAll(findRunnableSamplesInContent(it)) }
                }
            }

            is ContentDivergentInstance -> {
                content.before?.let { samples.addAll(findRunnableSamplesInContent(it)) }
                samples.addAll(findRunnableSamplesInContent(content.divergent))
                content.after?.let { samples.addAll(findRunnableSamplesInContent(it)) }
            }

            is ContentCodeInline -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentDRILink -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentResolvedLink -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentEmbeddedResource -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentTable -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentList -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is ContentGroup -> {
                content.children.forEach { child ->
                    samples.addAll(findRunnableSamplesInContent(child))
                }
            }

            is PlatformHintedContent -> {
                samples.addAll(findRunnableSamplesInContent(content.inner))
            }
        }

        return samples
    }
}
