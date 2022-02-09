package superFields

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.InheritedMember
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class PsiSuperFieldsTest : BaseAbstractTest() {

    @Disabled // TODO: Remove with Kotlin 1.6.20
    @Test
    fun `java inheriting java`() {
        testInline(
            """
            |/src/test/A.java
            |package test;
            |public class A {
            |    public int a = 1;
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
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
                    assertNotNull(this)
                    this.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        assertEquals(
                            DRI(packageName = "test", classNames = "A"),
                            this
                        )
                    }
                }
            }
        }
    }

    @Disabled // TODO: Remove with Kotlin 1.6.20
    @Test
    fun `java inheriting kotlin`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |open class A {
            |    var a: Int = 1
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
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
                    assertNotNull(this)
                    assertNotNull(this.getter)
                    assertNotNull(this.setter)
                    this.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        assertEquals(
                            DRI(packageName = "test", classNames = "A"),
                            this
                        )
                    }
                }
            }
        }
    }

    @Disabled // TODO: Remove with Kotlin 1.6.20
    @Test
    fun `java inheriting kotlin with @JvmField should not inherit beans`() {
        testInline(
            """
            |/src/test/A.kt
            |package test
            |open class A {
            |    @kotlin.jvm.JvmField
            |    var a: Int = 1
            |}
            |
            |/src/test/B.java
            |package test;
            |public class B extends A {}
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
                    assertNotNull(this)
                    assertNull(this.getter)
                    assertNull(this.setter)
                    this.extra[InheritedMember]?.inheritedFrom?.values?.single()?.run {
                        assertEquals(
                            DRI(packageName = "test", classNames = "A"),
                            this
                        )
                    }
                }
            }
        }
    }
}
