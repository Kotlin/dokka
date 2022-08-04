package translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import utils.assertNotNull

class DefaultPsiToDocumentableTranslatorTest : BaseAbstractTest() {
    @Suppress("DEPRECATION") // for includeNonPublic
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
                includeNonPublic = true
            }
        }
    }

    @Test
    fun `method overriding two documented classes picks closest class documentation`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample;
            |public class BaseClass2 extends BaseClass1 {
            |    /** B2 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass2 {
            |    void x() { }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfFunctionX = module.documentationOf("X", "x")
                assertTrue(
                    "B2" in documentationOfFunctionX,
                    "Expected nearest super method documentation to be parsed as documentation. " +
                            "Documentation: $documentationOfFunctionX"
                )
            }
        }
    }

    @Test
    fun `method overriding class and interface picks class documentation`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/Interface1.java
            |package sample;
            |public interface Interface1 {
            |    /** I1 */
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass1 implements Interface1 {
            |    void x() { }
            |}
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfFunctionX = module.documentationOf("X", "x")
                assertTrue(
                    "B1" in documentationOfFunctionX,
                    "Expected documentation of superclass being prioritized over interface " +
                            "Documentation: $documentationOfFunctionX"
                )
            }
        }
    }

    @Test
    fun `method overriding two classes picks closest documented class documentation`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample;
            |public class BaseClass2 extends BaseClass1 {
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass2 {
            |    void x() { }
            |}
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfFunctionX = module.documentationOf("X", "x")
                assertTrue(
                    "B1" in documentationOfFunctionX,
                    "Expected Documentation \"B1\", found: \"$documentationOfFunctionX\""
                )
            }
        }
    }

    @Test
    fun `java package-info package description`() {
        testInline(
            """
            |/src/main/java/sample/BaseClass1.java
            |package sample;
            |public class BaseClass1 {
            |    /** B1 */
            |    void x() { }
            |}
            |
            |/src/main/java/sample/BaseClass2.java
            |package sample;
            |public class BaseClass2 extends BaseClass1 {
            |    void x() {}
            |}
            |
            |/src/main/java/sample/X.java
            |package sample;
            |public class X extends BaseClass2 {
            |    void x() { }
            |}
            |
            |/src/main/java/sample/package-info.java
            |/**
            | * Here comes description from package-info
            | */
            |package sample;
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val documentationOfPackage = module.packages.single().documentation.values.single().children.single()
                    .firstMemberOfType<Text>().body
                assertEquals(
                    "Here comes description from package-info", documentationOfPackage
                )
            }
        }
    }

    @Test
    fun `java package-info package annotations`() {
        testInline(
            """
            |/src/main/java/sample/PackageAnnotation.java
            |package sample;
            |@java.lang.annotation.Target(java.lang.annotation.ElementType.PACKAGE)
            |public @interface PackageAnnotation {
            |}
            |
            |/src/main/java/sample/package-info.java
            |@PackageAnnotation
            |package sample;
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                assertEquals(
                    Annotations.Annotation(DRI("sample", "PackageAnnotation"), emptyMap()),
                    module.packages.single().extra[Annotations]?.directAnnotations?.values?.single()?.single()
                )
            }
        }
    }

    @Test
    fun `should add default value to constant properties`() {
        testInline(
            """
            |/src/main/java/test/JavaConstants.java
            |package test;
            |
            |public class JavaConstants {
            |    public static final byte BYTE = 1;
            |    public static final short SHORT = 2;
            |    public static final int INT = 3;
            |    public static final long LONG = 4L;
            |    public static final float FLOAT = 5.0f;
            |    public static final double DOUBLE = 6.0d;
            |    public static final String STRING = "Seven";
            |    public static final char CHAR = 'E';
            |    public static final boolean BOOLEAN = true;
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "JavaConstants" }

                val constants = testedClass.properties
                assertEquals(9, constants.size)

                val constantsByName = constants.associateBy { it.name }
                fun getConstantExpression(name: String): Expression? {
                    return constantsByName.getValue(name).extra[DefaultValue]?.expression?.values?.first()
                }

                assertEquals(IntegerConstant(1), getConstantExpression("BYTE"))
                assertEquals(IntegerConstant(2), getConstantExpression("SHORT"))
                assertEquals(IntegerConstant(3), getConstantExpression("INT"))
                assertEquals(IntegerConstant(4), getConstantExpression("LONG"))
                assertEquals(FloatConstant(5.0f), getConstantExpression("FLOAT"))
                assertEquals(DoubleConstant(6.0), getConstantExpression("DOUBLE"))
                assertEquals(StringConstant("Seven"), getConstantExpression("STRING"))
                assertEquals(StringConstant("E"), getConstantExpression("CHAR"))
                assertEquals(BooleanConstant(true), getConstantExpression("BOOLEAN"))
            }
        }
    }

    @Test
    fun `should resolve static imports used as annotation param values as literal values`() {
        testInline(
            """
            |/src/main/java/test/JavaClassUsingAnnotation.java
            |package test;
            |
            |import static test.JavaConstants.STRING;
            |import static test.JavaConstants.INTEGER;
            |import static test.JavaConstants.LONG;
            |import static test.JavaConstants.BOOLEAN;
            |import static test.JavaConstants.DOUBLE;
            |import static test.JavaConstants.FLOAT;
            |
            |@JavaAnnotation(
            |        stringValue = STRING, intValue = INTEGER, longValue = LONG,
            |        booleanValue = BOOLEAN, doubleValue = DOUBLE, floatValue = FLOAT
            |)
            |public class JavaClassUsingAnnotation {
            |}
            |
            |/src/main/java/test/JavaAnnotation.java
            |package test;
            |@Documented
            |public @interface JavaAnnotation {
            |    String stringValue();
            |    int intValue();
            |    long longValue();
            |    boolean booleanValue();
            |    double doubleValue();
            |    float floatValue();
            |}
            |
            |/src/main/java/test/JavaConstants.java
            |package test;
            |public class JavaConstants {
            |    public static final String STRING = "STRING_CONSTANT_VALUE";
            |    public static final int INTEGER = 5;
            |    public static final long LONG = 6L;
            |    public static final boolean BOOLEAN = true;
            |    public static final double DOUBLE = 7.0d;
            |    public static final float FLOAT = 8.0f;
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "JavaClassUsingAnnotation" }

                val annotation = (testedClass as DClass).extra[Annotations]?.directAnnotations?.values?.single()?.single()
                checkNotNull(annotation)

                assertEquals("JavaAnnotation", annotation.dri.classNames)
                assertEquals(StringValue("STRING_CONSTANT_VALUE"), annotation.params["stringValue"])

                assertEquals(IntValue(5), annotation.params["intValue"])
                assertEquals(LongValue(6), annotation.params["longValue"])
                assertEquals(BooleanValue(true), annotation.params["booleanValue"])
                assertEquals(DoubleValue(7.0), annotation.params["doubleValue"])
                assertEquals(FloatValue(8.0f), annotation.params["floatValue"])
            }
        }
    }

    class OnlyPsiPlugin : DokkaPlugin() {
        private val dokkaBase by lazy { plugin<DokkaBase>() }

        @Suppress("unused")
        val psiOverrideDescriptorTranslator by extending {
            (dokkaBase.psiToDocumentableTranslator
                    override dokkaBase.descriptorToDocumentableTranslator)
        }
    }

    // for Kotlin classes from DefaultPsiToDocumentableTranslator
    @Test
    fun `should resolve ultralight class`() {
        val configurationWithNoJVM = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/example/Test.kt
            |package example
            |
            |open class KotlinSubClass {
            |    fun kotlinSubclassFunction(bar: String): String {
            |       return "KotlinSubClass"
            |    }
            |}
            |
            |/src/main/java/example/JavaLeafClass.java
            |package example;
            |
            |public class JavaLeafClass extends KotlinSubClass {
            |    public String javaLeafClassFunction(String baz) {
            |        return "JavaLeafClass";
            |    }
            |}
        """.trimMargin(),
            configurationWithNoJVM,
            pluginOverrides = listOf(OnlyPsiPlugin()) // suppress a descriptor translator because of psi and descriptor translators work in parallel
        ) {
            documentablesMergingStage = { module ->
                val kotlinSubclassFunction =
                    module.packages.single().classlikes.find { it.name == "JavaLeafClass" }?.functions?.find { it.name == "kotlinSubclassFunction" }
                        .assertNotNull("kotlinSubclassFunction ")

                assertEquals(
                    "String",
                    (kotlinSubclassFunction.type as? TypeConstructor)?.dri?.classNames
                )
                assertEquals(
                    "String",
                    (kotlinSubclassFunction.parameters.firstOrNull()?.type as? TypeConstructor)?.dri?.classNames
                )
            }
        }
    }

    @Test
    fun `should preserve regular functions that look like accessors, but are not accessors`() {
        testInline(
            """
            |/src/main/java/test/A.java
            |package test;
            |public class A {
            |    public int a = 1;
            |    public void setA() { } // no arg
            |    public String getA() { return "s"; } // wrong return type
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testClass = module.packages.single().classlikes.single { it.name == "A" }

                val setterLookalike = testClass.functions.firstOrNull { it.name == "setA" }
                assertNotNull(setterLookalike) {
                    "Expected regular function not found, wrongly categorized as setter?"
                }

                val getterLookalike = testClass.functions.firstOrNull { it.name == "getA" }
                assertNotNull(getterLookalike) {
                    "Expected regular function not found, wrongly categorized as getter?"
                }
            }
        }
    }

    @Test
    fun `should not associate accessors with field because field is public api`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    documentedVisibilities = setOf(
                        DokkaConfiguration.Visibility.PUBLIC,
                        DokkaConfiguration.Visibility.PROTECTED
                    )
                }
            }
        }

        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   protected int a = 1;
            |   public int getA() { return a; }
            |   public void setA(int a) { this.a = a; }
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "A" }

                val property = testedClass.properties.single { it.name == "a" }
                assertEquals(JavaVisibility.Protected, property.visibility.values.single())
                assertNull(property.getter)
                assertNull(property.setter)

                assertEquals(2, testedClass.functions.size)

                assertEquals("getA", testedClass.functions[0].name)
                assertEquals("setA", testedClass.functions[1].name)
            }
        }
    }

    @Test
    fun `should add IsVar extra for field with getter and setter`() {
        testInline(
            """
            |/src/main/java/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public int getA() { return a; }
            |   public void setA(int a) { this.a = a; }
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "A" }

                val property = testedClass.properties.single { it.name == "a" }
                assertNotNull(property.extra[IsVar])
            }
        }
    }

    @Test
    fun `should not add IsVar extra if field does not have a setter`() {
        testInline(
            """
            |/src/main/java/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   public int getA() { return a; }
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "A" }

                val property = testedClass.properties.single { it.name == "a" }
                assertNull(property.extra[IsVar])
            }
        }
    }

    @Test
    fun `should add IsVar for non-final java field without any accessors`() {
        testInline(
            """
            |/src/main/java/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "A" }

                val property = testedClass.properties.single { it.name == "a" }
                assertNotNull(property.extra[IsVar])
            }
        }
    }

    @Test
    fun `should not add IsVar for final java field`() {
        testInline(
            """
            |/src/main/java/test/A.java
            |package test;
            |public class A {
            |   public final int a = 2;
            |}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "A" }

                val publicFinal = testedClass.properties.single { it.name == "a" }
                assertNull(publicFinal.extra[IsVar])
            }
        }
    }
}
