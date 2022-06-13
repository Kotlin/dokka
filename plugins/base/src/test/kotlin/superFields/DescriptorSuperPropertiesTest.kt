package superFields

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.InheritedMember
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DescriptorSuperPropertiesTest : BaseAbstractTest() {

    private val commonTestConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                name = "jvm"
            }
        }
    }

    @Test
    fun `kotlin inheriting java should append only getter`() {
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
            commonTestConfiguration
        ) {
            this.documentablesTransformationStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties

                val property = kotlinProperties.single { it.name == "a" }
                val propertyInheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), propertyInheritedFrom)

                assertNull(property.setter)
                assertNotNull(property.getter)

                val getterInheritedFrom = property.getter?.extra?.get(InheritedMember)?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), getterInheritedFrom)
            }
        }
    }


    @Test
    fun `kotlin inheriting java should ignore setter lookalike for non accessible field`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private int a = 1;
            |   
            |   public void setA(int a) { this.a = a; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "B" }

                val property = testedClass.properties.firstOrNull { it.name == "a" }
                assertNull(property, "Inherited property `a` should not be visible as it's not accessible")

                val setterLookalike = testedClass.functions.firstOrNull { it.name == "setA" }
                assertNotNull(setterLookalike) {
                    "Expected setA to be a regular function because field `a` is neither var nor val from Kotlin's " +
                            "interop perspective, it's not accessible."
                }
            }
        }
    }


    @Test
    fun `kotlin inheriting java should append getter and setter`() {
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
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val property = kotlinProperties.single { it.name == "a" }
                property.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                    assertEquals(
                        DRI(packageName = "test", classNames = "A"),
                        this
                    )
                }

                val getter = property.getter
                assertNotNull(getter)
                assertEquals("getA", getter.name)
                val getterInheritedFrom = getter.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), getterInheritedFrom)

                val setter = property.setter
                assertNotNull(setter)
                assertEquals("setA", setter.name)
                val setterInheritedFrom = setter.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), setterInheritedFrom)
            }
        }
    }

    @Test
    fun `should have special getter and setter names for boolean property inherited from java`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |   private boolean bool = true;
            |   public boolean isBool() { return bool; }
            |   public void setBool(boolean bool) { this.bool = bool; }
            |}
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val kotlinProperties = module.packages.single().classlikes.single { it.name == "B" }.properties
                val boolProperty = kotlinProperties.single { it.name == "bool" }

                val getter = boolProperty.getter
                assertNotNull(getter)
                assertEquals("isBool", getter.name)

                val setter = boolProperty.setter
                assertNotNull(setter)
                assertEquals("setBool", setter.name)
            }
        }
    }

    @Test
    fun `kotlin inheriting java should not append anything since field is public api`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "jvm"
                    name = "jvm"
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
            |
            |/src/test/B.kt
            |package test
            |class B : A {}
        """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val testedClass = module.packages.single().classlikes.single { it.name == "B" }
                val property = testedClass.properties.single { it.name == "a" }

                assertNull(property.getter)
                assertNull(property.setter)
                assertEquals(2, testedClass.functions.size)

                assertEquals("getA", testedClass.functions[0].name)
                assertEquals("setA", testedClass.functions[1].name)

                val inheritedFrom = property.extra[InheritedMember]?.inheritedFrom?.values?.single()
                assertEquals(DRI(packageName = "test", classNames = "A"), inheritedFrom)
            }
        }
    }

    @Test
    fun `should preserve regular functions that look like accessors, but are not accessors`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |class A {
            |    private var v: Int = 0
            |    
            |    // not accessors because declared separately, just functions
            |    fun setV(new: Int) { v = new }
            |    fun getV(): Int = v
            |}
        """.trimIndent(),
            commonTestConfiguration
        ) {
            documentablesMergingStage = { module ->
                val testClass = module.packages.single().classlikes.single { it.name == "A" }
                val setterLookalike = testClass.functions.firstOrNull { it.name == "setV" }
                assertNotNull(setterLookalike) {
                    "Expected regular function not found, wrongly categorized as setter?"
                }

                val getterLookalike = testClass.functions.firstOrNull { it.name == "getV" }
                assertNotNull(getterLookalike) {
                    "Expected regular function not found, wrongly categorized as getter?"
                }
            }
        }
    }
}
