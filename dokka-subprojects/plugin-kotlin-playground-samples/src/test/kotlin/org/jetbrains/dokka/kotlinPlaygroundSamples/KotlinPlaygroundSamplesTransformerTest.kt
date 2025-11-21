/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinPlaygroundSamples

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.pages.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinPlaygroundSamplesTransformerTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `KotlinPlaygroundSamplesTransformer is applied`() {
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
            pluginsSetupStage = { context ->
                val pageTransformers = context[CoreExtensions.pageTransformer]
                assertTrue(
                    pageTransformers.any { it is KotlinPlaygroundSamplesTransformer },
                    "KotlinPlaygroundSamplesTransformer should be registered on PageTransformer extension point"
                )
            }
        }
    }

    @Test
    fun `page should contain correct embedded resources`() {
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
            pagesTransformationStage = { root ->
                val contentPages = root.children.filterIsInstance<ContentPage>()

                val defaultKotlinPlaygroundScriptIncluded = contentPages.all {
                    KotlinPlaygroundSamplesConfiguration.defaultKotlinPlaygroundScript in it.embeddedResources
                }
                assertTrue(defaultKotlinPlaygroundScriptIncluded, "Default Kotlin Playground script should be included")

                val kotlinPlaygroundSamplesScriptIncluded = contentPages.all {
                    "scripts/kotlin-playground-samples.js" in it.embeddedResources
                }

                assertTrue(kotlinPlaygroundSamplesScriptIncluded, "Kotlin Playground Samples script should be included")

                val kotlinPlaygroundSamplesStyleIncluded = contentPages.all {
                    "styles/kotlin-playground-samples.css" in it.embeddedResources
                }

                assertTrue(kotlinPlaygroundSamplesStyleIncluded, "Kotlin Playground Samples style should be included")
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
                val kotlinPlaygroundSamples = findKotlinPlaygroundSamples(rootPageNode)
                assertEquals(1, kotlinPlaygroundSamples.size)
                val kotlinPlaygroundSample = kotlinPlaygroundSamples[0]
                assertEquals("kotlin", kotlinPlaygroundSample.language)
                assertEquals(setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace), kotlinPlaygroundSample.style)
                val child = kotlinPlaygroundSample.children[0]
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
                val kotlinPlaygroundSamples = findKotlinPlaygroundSamples(rootPageNode)
                assertEquals(2, kotlinPlaygroundSamples.size)

                // Verify first sample
                val firstSample = kotlinPlaygroundSamples[0]
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
                val secondSample = kotlinPlaygroundSamples[1]
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
                val kotlinPlaygroundSamples = findKotlinPlaygroundSamples(rootPageNode)
                assertEquals(3, kotlinPlaygroundSamples.size)

                // Verify first sample
                val firstSample = kotlinPlaygroundSamples[0]
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
                val secondSample = kotlinPlaygroundSamples[1]
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
                val thirdSample = kotlinPlaygroundSamples[2]
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
                val kotlinPlaygroundSamples = findKotlinPlaygroundSamples(rootPageNode)
                assertEquals(1, kotlinPlaygroundSamples.size)

                val kotlinPlaygroundSample = kotlinPlaygroundSamples[0]
                assertEquals("kotlin", kotlinPlaygroundSample.language)
                assertEquals(
                    setOf<Style>(ContentStyle.RunnableSample, TextStyle.Monospace),
                    kotlinPlaygroundSample.style
                )

                val child = kotlinPlaygroundSample.children[0]
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

    private fun findKotlinPlaygroundSamples(node: PageNode): List<ContentCodeBlock> {
        val samples = mutableListOf<ContentCodeBlock>()

        when (node) {
            is ContentPage -> {
                samples.addAll(findKotlinPlaygroundSamplesInContent(node.content))
            }
        }

        node.children.forEach { child ->
            samples.addAll(findKotlinPlaygroundSamples(child))
        }

        return samples
    }

    private fun findKotlinPlaygroundSamplesInContent(content: ContentNode): List<ContentCodeBlock> {
        val samples = mutableListOf<ContentCodeBlock>()

        when (content) {
            is ContentCodeBlock -> {
                if (content.style.contains(ContentStyle.RunnableSample)) {
                    samples.add(content)
                }
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentHeader -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentDivergentGroup -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child.divergent))
                    child.before?.let { samples.addAll(findKotlinPlaygroundSamplesInContent(it)) }
                    child.after?.let { samples.addAll(findKotlinPlaygroundSamplesInContent(it)) }
                }
            }

            is ContentDivergentInstance -> {
                content.before?.let { samples.addAll(findKotlinPlaygroundSamplesInContent(it)) }
                samples.addAll(findKotlinPlaygroundSamplesInContent(content.divergent))
                content.after?.let { samples.addAll(findKotlinPlaygroundSamplesInContent(it)) }
            }

            is ContentCodeInline -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentDRILink -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentResolvedLink -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentEmbeddedResource -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentTable -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentList -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is ContentGroup -> {
                content.children.forEach { child ->
                    samples.addAll(findKotlinPlaygroundSamplesInContent(child))
                }
            }

            is PlatformHintedContent -> {
                samples.addAll(findKotlinPlaygroundSamplesInContent(content.inner))
            }
        }

        return samples
    }
}
