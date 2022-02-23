package superFields

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.links.TypeReference
import org.jetbrains.dokka.model.InheritedMember
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DescriptorSuperPropertiesTest : BaseAbstractTest() {

    @Test
    fun `kotlin inheriting java should append getter`() {
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
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        analysisPlatform = "jvm"
                        name = "jvm"
                    }
                }
            }
        ) {
            this.documentablesTransformationStage = {
                it.packages.single().classlikes.single { it.name == "B" }.properties.single { it.name == "a" }.run {
                    Assertions.assertNotNull(this)
                    Assertions.assertNotNull(this.getter)
                    Assertions.assertNull(this.setter)
                    this.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        assertEquals(
                            DRI(packageName = "test", classNames = "A", callable = Callable("a", params = emptyList())),
                            this
                        )
                    }
                    this.getter.run {
                        this!!.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                            assertEquals(
                                DRI(packageName = "test", classNames = "A", callable = Callable("getA", params = emptyList())),
                                this
                            )
                        }
                    }
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
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        analysisPlatform = "jvm"
                        name = "jvm"
                    }
                }
            }
        ) {
            documentablesMergingStage = {
                it.packages.single().classlikes.single { it.name == "B" }.properties.single { it.name == "a" }.run {
                    Assertions.assertNotNull(this)
                    Assertions.assertNotNull(this.getter)
                    Assertions.assertNotNull(this.setter)
                    this.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        assertEquals(
                            DRI(packageName = "test", classNames = "A", callable = Callable("a", params = emptyList())),
                            this
                        )
                    }
                    this.getter.run {
                        this!!.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                            assertEquals(
                                DRI(packageName = "test", classNames = "A", callable = Callable("getA", params = emptyList())),
                                this
                            )
                        }
                    }
                    this.setter.run {
                        this!!.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                            assertEquals(
                                DRI(packageName = "test", classNames = "A",
                                    callable = Callable("setA",
                                        params = listOf(TypeConstructor("kotlin.Int", emptyList()))
                                    )
                                ),
                                this
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `kotlin inheriting java should not append anything since field is public`() {
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
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        analysisPlatform = "jvm"
                        name = "jvm"
                        classpath += jvmStdlibPath!!
                    }
                }
            }
        ) {
            documentablesMergingStage = {
                it.packages.single().classlikes.single { it.name == "B" }.properties.single { it.name == "a" }.run {
                    Assertions.assertNotNull(this)
                    Assertions.assertNull(this.getter)
                    Assertions.assertNull(this.setter)
                    this.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        assertEquals(
                            DRI(packageName = "test", classNames = "A", callable = Callable("a", params = emptyList())),
                            this
                        )
                    }
                }
            }
        }
    }
}