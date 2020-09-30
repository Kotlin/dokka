package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.dokka.utilities.cast
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JavadocLinkingTest : AbstractCoreTest() {

    @Test
    fun `linebroken link`() {
        fun externalLink(link: String) = ExternalDocumentationLink(link)

        val config = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("jvmSrc/")
                    externalDocumentationLinks = listOf(
                        externalLink("https://docs.oracle.com/javase/8/docs/api/"),
                        externalLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
                    )
                    analysisPlatform = "jvm"
                }
            }
        }
        testInline(
            """
            |/jvmSrc/javadoc/test/SomeClass.kt
            |
            |package example
            |
            |class SomeClass {
            |    fun someFun(x: Int): Int = 1
            |}
            |/jvmSrc/javadoc/test/SomeJavaDocExample.java
            |
            |package example;
            |
            |/**
            | * Here comes some comment
            | *
            | * {@link example.SomeClass#someFun(int) someName(ads,
            | * dsa)}
            | *
            | * longer comment
            | */
            |public class SomeJavaDocExample {
            |    public void someFunc(int integer, Object object) {
            |    }
            |}
        """.trimIndent(),
            config,
            cleanupOutput = false
        ) {
            documentablesMergingStage = {
                it.packages.single().classlikes.single { it.name == "SomeJavaDocExample" }.documentation.values.single().children.single().children.single {
                    it is DocumentationLink
                }.children.single().cast<Text>().body.run {
                    assertEquals("someName(ads, dsa)", this)
                }
            }
        }
    }

}
