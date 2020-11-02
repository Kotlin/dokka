package org.jetbrains.dokka.javadoc.location

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.jdk
import org.jetbrains.dokka.kotlinStdlib
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.utilities.cast
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin
import kotlin.test.assertEquals

class JavadocLinkingTest : BaseAbstractTest() {

    @Test
    fun lineBrokenLink() {
        val config = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("jvmSrc/")
                    externalDocumentationLinks = listOf(
                        DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                        DokkaConfiguration.ExternalDocumentationLink.kotlinStdlib(),
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
            |
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
        """.trimMargin(),
            config,
            pluginOverrides = listOf(TestOutputWriterPlugin())
        ) {
            documentablesMergingStage = {
                it.packages.single()
                    .classlikes.single { classlike -> classlike.name == "SomeJavaDocExample" }
                    .documentation.values.single()
                    .children.single()
                    .children.single()
                    .children.single {
                        it is DocumentationLink
                    }.children.filterIsInstance<Text>().single { it.body.contains("someName") }.cast<Text>().body.run {
                        assertEquals("someName(ads, dsa)", this)
                    }
            }
        }
    }
}
