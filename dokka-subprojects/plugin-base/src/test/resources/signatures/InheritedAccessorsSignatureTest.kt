/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import signatures.renderedContent
import signatures.signature
import utils.A
import utils.Span
import utils.TestOutputWriterPlugin
import utils.match
import utils.OnlyDescriptors
import utils.OnlyJavaPsi
import utils.match
import kotlin.collections.toList
import kotlin.let
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.trimIndent

class InheritedAccessorsSignatureTest : org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest() {

    private val configuration = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
        testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
            testApi.testRunner.SourceSetsBuilder.sourceSet {
                sourceRoots = kotlin.collections.listOf("src/")
                classpath = kotlin.collections.listOf(
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath
                        ?: throw kotlin.IllegalStateException("Common stdlib is not found"),
                    org.jetbrains.dokka.testApi.testRunner.AbstractTest.jvmStdlibPath
                        ?: throw kotlin.IllegalStateException("JVM stdlib is not found")
                )
                externalDocumentationLinks =
                    kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
            }
        }
    }

    @utils.OnlyDescriptors("'var' expected but found: 'open var'")
    @kotlin.test.Test
    fun `should collapse accessor functions inherited from java into the property`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public int getA() { return a; }
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-b/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3, signatures.size,
                        "Expected 3 signatures: class signature, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "var ", utils.A("a"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-a/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3, signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "open var ", utils.A("a"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @utils.OnlyDescriptors("'var' expected but found: 'open var'")
    @kotlin.test.Test
    fun `should render as val if inherited java property has no setter`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public int getA() { return a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-b/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "val ", utils.A("a"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-a/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "open val ", utils.A("a"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @kotlin.test.Test
    fun `should keep inherited java setter as a regular function due to inaccessible property`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-b/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, constructor and setter"
                    )

                    val setterFunction = signatures[2]
                    setterFunction.match(
                        "open fun ", utils.A("setA"), "(", signatures.Parameters(
                            signatures.Parameter("a: ", utils.A("Int"))
                        ), ")",
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-a/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and setter"
                    )

                    val setterFunction = signatures[2]
                    setterFunction.match(
                        "open fun ", utils.A("setA"), "(", signatures.Parameters(
                            signatures.Parameter("a: ", utils.A("Int"))
                        ), ")",
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @utils.OnlyDescriptors("'var' expected but found: 'open var'")
    @kotlin.test.Test
    fun `should keep inherited java accessor lookalikes if underlying function is public`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   public int a = 1;
            |   public int getA() { return a; }
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("root/test/-b/index.html").signature().toList()
                kotlin.test.assertEquals(
                    5, signatures.size,
                    "Expected 5 signatures: class signature, constructor, property and two accessor lookalikes"
                )

                val getterLookalikeFunction = signatures[3]
                getterLookalikeFunction.match(
                    "open fun ", utils.A("getA"), "():", utils.A("Int"),
                    ignoreSpanWithTokenStyle = true
                )

                val setterLookalikeFunction = signatures[4]
                setterLookalikeFunction.match(
                    "open fun ", utils.A("setA"), "(", signatures.Parameters(
                        signatures.Parameter("a: ", utils.A("Int"))
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )

                val property = signatures[2]
                property.match(
                    "var ", utils.A("a"), ":", utils.A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

    @utils.OnlyJavaPsi
    @kotlin.test.Test
    fun `should keep kotlin property with no accessors when java inherits kotlin a var`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected to find 3 signatures: class, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "open var ", utils.A("variable"), ": ", utils.Span("String"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @utils.OnlyJavaPsi
    @kotlin.test.Test
    fun `kotlin property with compute get and set`() {
        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
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
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected to find 3 signatures: class, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "var ", utils.A("variable"), ": ", utils.A("String"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                // it's actually unclear how it should react in this situation. It should most likely not
                // break the abstraction and display it as a simple variable just like can be seen from Kotlin,
                // test added to control changes
                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        4,
                        signatures.size,
                        "Expected to find 4 signatures: class, default constructor and two accessors"
                    )

                    val getter = signatures[2]
                    getter.match(
                        "fun ", utils.A("getVariable"), "(): ", utils.Span("String"),
                        ignoreSpanWithTokenStyle = true
                    )

                    val setter = signatures[3]
                    setter.match(
                        "fun ", utils.A("setVariable"), "(", signatures.Parameters(
                            signatures.Parameter("value: ", utils.Span("String"))
                        ), ")",
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @utils.OnlyDescriptors("'var' expected but found: 'open var'")
    @kotlin.test.Test
    fun `inherited property should inherit getter's visibility`() {
        val configWithProtectedVisibility = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
            testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
                testApi.testRunner.SourceSetsBuilder.sourceSet {
                    sourceRoots = kotlin.collections.listOf("src/")
                    classpath = kotlin.collections.listOf(
                        org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath
                            ?: throw kotlin.IllegalStateException("Common stdlib is not found"),
                        org.jetbrains.dokka.testApi.testRunner.AbstractTest.jvmStdlibPath
                            ?: throw kotlin.IllegalStateException("JVM stdlib is not found")
                    )
                    externalDocumentationLinks =
                        kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
                    documentedVisibilities = kotlin.collections.setOf(
                        org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                        org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/test/JavaClass.java
            |package test;
            |public class JavaClass {
            |    private int protectedGetterAndProtectedSetter = 0;
            |
            |    protected int getProtectedGetterAndProtectedSetter() {
            |        return protectedGetterAndProtectedSetter;
            |    }
            |
            |    protected void setProtectedGetterAndProtectedSetter(int protectedGetterAndProtectedSetter) {
            |        this.protectedGetterAndProtectedSetter = protectedGetterAndProtectedSetter;
            |    }
            |}
            |
            |/src/test/KotlinClass.kt
            |package test
            |open class KotlinClass : JavaClass() { }
        """.trimIndent(),
            configWithProtectedVisibility,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "protected var ", utils.A("protectedGetterAndProtectedSetter"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "protected open var ", utils.A("protectedGetterAndProtectedSetter"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @utils.OnlyDescriptors("'var' expected but found: 'open var'")
    @kotlin.test.Test
    fun `should resolve protected java property as protected`() {
        val configWithProtectedVisibility = org.jetbrains.dokka.testApi.testRunner.AbstractTest.dokkaConfiguration {
            testApi.testRunner.TestDokkaConfigurationBuilder.sourceSets {
                testApi.testRunner.SourceSetsBuilder.sourceSet {
                    sourceRoots = kotlin.collections.listOf("src/")
                    classpath = kotlin.collections.listOf(
                        org.jetbrains.dokka.testApi.testRunner.AbstractTest.commonStdlibPath
                            ?: throw kotlin.IllegalStateException("Common stdlib is not found"),
                        org.jetbrains.dokka.testApi.testRunner.AbstractTest.jvmStdlibPath
                            ?: throw kotlin.IllegalStateException("JVM stdlib is not found")
                    )
                    externalDocumentationLinks =
                        kotlin.collections.listOf(org.jetbrains.dokka.testApi.testRunner.AbstractTest.stdlibExternalDocumentationLink)
                    documentedVisibilities = kotlin.collections.setOf(
                        org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                        org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        val writerPlugin = utils.TestOutputWriterPlugin()
        org.jetbrains.dokka.testApi.testRunner.AbstractTest.testInline(
            """
            |/src/test/JavaClass.java
            |package test;
            |public class JavaClass {
            |    protected int protectedProperty = 0;
            |}
            |
            |/src/test/KotlinClass.kt
            |package test
            |open class KotlinClass : JavaClass() { }
        """.trimIndent(),
            configWithProtectedVisibility,
            pluginOverrides = kotlin.collections.listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    kotlin.test.assertEquals(
                        3,
                        signatures.size,
                        "Expected 2 signatures: class signature, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "protected var ", utils.A("protectedProperty"), ":", utils.A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }
}
