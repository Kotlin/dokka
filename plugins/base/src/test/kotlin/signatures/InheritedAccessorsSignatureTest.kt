package signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import utils.A
import utils.Span
import utils.TestOutputWriterPlugin
import utils.match
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
    fun `should collapse accessor functions inherited from java into the property`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
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
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-b/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(
                        3, signatures.size,
                        "Expected 3 signatures: class signature, constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "var ", A("a"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-a/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    assertEquals(
                        3, signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "open var ", A("a"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @Test
    fun `should render as val if inherited java property has no setter`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
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
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-b/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected 3 signatures: class signature, constructor and property")

                    val property = signatures[2]
                    property.match(
                        "val ", A("a"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-a/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "open val ", A("a"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
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

    @Test
    fun `should keep inherited java accessor lookalikes if underlying function is public`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
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
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                val signatures = writerPlugin.writer.renderedContent("root/test/-b/index.html").signature().toList()
                assertEquals(
                    5, signatures.size,
                    "Expected 5 signatures: class signature, constructor, property and two accessor lookalikes"
                )

                val getterLookalikeFunction = signatures[3]
                getterLookalikeFunction.match(
                    "open fun ", A("getA"), "():", A("Int"),
                    ignoreSpanWithTokenStyle = true
                )

                val setterLookalikeFunction = signatures[4]
                setterLookalikeFunction.match(
                    "open fun ", A("setA"), "(", Parameters(
                        Parameter("a: ", A("Int"))
                    ), ")",
                    ignoreSpanWithTokenStyle = true
                )

                val property = signatures[2]
                property.match(
                    "var ", A("a"), ":", A("Int"),
                    ignoreSpanWithTokenStyle = true
                )
            }
        }
    }

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
                        "open var ", A("variable"), ": ", Span("String"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

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

                // it's actually unclear how it should react in this situation. It should most likely not
                // break the abstraction and display it as a simple variable just like can be seen from Kotlin,
                // test added to control changes
                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    assertEquals(
                        4,
                        signatures.size,
                        "Expected to find 4 signatures: class, default constructor and two accessors"
                    )

                    val getter = signatures[2]
                    getter.match(
                        "fun ", A("getVariable"), "(): ", Span("String"),
                        ignoreSpanWithTokenStyle = true
                    )

                    val setter = signatures[3]
                    setter.match(
                        "fun ", A("setVariable"), "(", Parameters(
                            Parameter("value: ", Span("String"))
                        ), ")",
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @Test
    fun `inherited property should inherit getter's visibility`() {
        val configWithProtectedVisibility = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    classpath = listOf(
                        commonStdlibPath ?: throw IllegalStateException("Common stdlib is not found"),
                        jvmStdlibPath ?: throw IllegalStateException("JVM stdlib is not found")
                    )
                    externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
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
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected 3 signatures: class signature, constructor and property")

                    val property = signatures[2]
                    property.match(
                        "protected var ", A("protectedGetterAndProtectedSetter"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }

                writerPlugin.writer.renderedContent("root/test/-java-class/index.html").let { javaClassContent ->
                    val signatures = javaClassContent.signature().toList()
                    assertEquals(
                        3,
                        signatures.size,
                        "Expected 3 signatures: class signature, default constructor and property"
                    )

                    val property = signatures[2]
                    property.match(
                        "protected open var ", A("protectedGetterAndProtectedSetter"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }

    @Test
    fun `should resolve protected java property as protected`() {
        val configWithProtectedVisibility = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    classpath = listOf(
                        commonStdlibPath ?: throw IllegalStateException("Common stdlib is not found"),
                        jvmStdlibPath ?: throw IllegalStateException("JVM stdlib is not found")
                    )
                    externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()
        testInline(
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
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/test/-kotlin-class/index.html").let { kotlinClassContent ->
                    val signatures = kotlinClassContent.signature().toList()
                    assertEquals(3, signatures.size, "Expected 2 signatures: class signature, constructor and property")

                    val property = signatures[2]
                    property.match(
                        "protected var ", A("protectedProperty"), ":", A("Int"),
                        ignoreSpanWithTokenStyle = true
                    )
                }
            }
        }
    }
}
