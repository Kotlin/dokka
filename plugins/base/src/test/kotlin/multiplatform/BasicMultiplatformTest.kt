package multiplatform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest

class BasicMultiplatformTest : BaseAbstractTest() {

    @Test
    fun dataTestExample() {
        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("$testDataDir/jvmMain/")
                }
            }
        }

        testFromData(configuration) {
            pagesTransformationStage = {
                assertEquals(7, it.children.firstOrNull()?.children?.count() ?: 0)
            }
        }
    }

    @Test
    fun dataTestLongSparseArray() {
        val testDataDir = getTestDataDir("multiplatform/longSparseArray").toAbsolutePath()

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    name = "jvm"
                    displayName = "JVM"
                    sourceRoots = listOf("$testDataDir/jvmMain/")
                }
            }
            sourceSets {
                sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("$testDataDir/commonMain/")
                }
            }
            sourceSets {
                sourceSet {
                    name = "native"
                    displayName = "Native"
                    analysisPlatform = "native"
                    sourceRoots = listOf("$testDataDir/nativeMain/")
                }
            }
        }

        testFromData(configuration) {
            documentablesCreationStage = {
                assertEquals(1, it[0].packages.size)
                assertEquals(1, it[0].packages[0].classlikes.size)
            }
        }
    }

    @Test
    fun inlineTestExample() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/multiplatform/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/multiplatform/Test.kt
            |package multiplatform
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = {
                assertEquals(3, it.parentMap.size)
            }
        }
    }
}
