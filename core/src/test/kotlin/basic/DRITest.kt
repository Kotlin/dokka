package basic

import org.jetbrains.dokka.links.*
import org.junit.Assert.assertEquals
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class DRITest: AbstractCoreTest() {
    @Test
    fun `#634`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package toplevel
            |
            |inline fun <T, R : Comparable<R>> Array<out T>.mySortBy(
            |    crossinline selector: (T) -> R?): Array<out T> = TODO()
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val expected = TypeConstructor("kotlin.Function1", listOf(
                    TypeParam(listOf(Nullable(TypeConstructor("kotlin.Any", emptyList())))),
                    Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(SelfType)))))
                ))
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.params?.single()
                assertEquals(expected, actual)
            }
        }
    }

}