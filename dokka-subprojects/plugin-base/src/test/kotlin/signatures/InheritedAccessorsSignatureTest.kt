/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import utils.*
import kotlin.test.Test
import kotlin.test.assertEquals

class InheritedAccessorsSignatureTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath = listOf(
                    commonStdlibPath ?: throw IllegalStateException("Common stdlib is not found"),
                    jvmStdlibPath ?: throw IllegalStateException("JVM stdlib is not found")
                )
                externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
            }
        }
    }

    @Test
    fun `should keep inherited java setter as a regular function due to inaccessible property`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-b/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected 3 signatures: class signature, constructor and setter")

                    val setterFunction = signatures[2]
                    setterFunction.match(
                        "open fun ", A("setA"), "(", Parameters(
                            Parameter("a: ", A("Int"))
                        ), ")",
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-a/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and setter"
                    )

                    val setterFunction = signatures[2]
                    setterFunction.match(
                        "open fun ", A("setA"), "(", Parameters(
                            Parameter("a: ", A("Int"))
                        ), ")",
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @OnlyJavaSymbols("PSI treats the property as open - which is wrong")
    @Test
    fun `should keep kotlin property with no accessors when java inherits kotlin a var`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/test/JavaClass.java
            |package test;
            |public class JavaClass extends KotlinClass {}
            |
            |/src/test/KotlinClass.kt
            |package test
            |open class KotlinClass {
            |    var variable: String = "s"
            |}
        """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(
                        3,
                        signatures.size,
                        "Expected to find 3 signatures: class, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "var ", A("variable"), ": ", A("String"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @OnlyJavaSymbols("AA returns a property even for Java classes - seems correct")
    @Test
    fun `kotlin property with compute get and set`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            """
            |/src/test/JavaClass.java
            |package test;
            |public class JavaClass extends KotlinClass {}
            |
            |/src/test/KotlinClass.kt
            |package test
            |open class KotlinClass {
            |    var variable: String
            |        get() = "asd"
            |        set(value) {}
            |}
        """.trimIndent(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected to find 3 signatures: class, constructor and property")

                    val property = signatures[2]
                    property.match(
                        "var ", A("variable"), ": ", A("String"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                // AA returns a property - which seems correct, as we show "Kotlin API"?
                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected to find 3 signatures: class, constructor and property")

                    val property = signatures[2]
                    property.match(
                        "var ", A("variable"), ": ", A("String"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

}
