package basic

import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.asSequence
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

class DRITest : AbstractCoreTest() {
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
                val expected = TypeConstructor(
                    "kotlin.Function1", listOf(
                        TypeParam(listOf(Nullable(TypeConstructor("kotlin.Any", emptyList())))),
                        Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(SelfType)))))
                    )
                )
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.params?.single()
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun `#634 with immediate nullable self`() {
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
            |fun <T : Comparable<T>> Array<T>.doSomething(t: T?): Array<T> = TODO()
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val expected = Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(SelfType)))))
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.params?.single()
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun `#634 with generic nullable receiver`() {
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
            |fun <T : Comparable<T>> T?.doSomethingWithNullable() = TODO()
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val expected = Nullable(TypeParam(listOf(TypeConstructor("kotlin.Comparable", listOf(SelfType)))))
                val actual = module.packages.single()
                    .functions.single()
                    .dri.callable?.receiver
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun `#642 with * and Any?`() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    analysisPlatform = "js"
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/Test.kt
            |
            |open class Bar<Z>
            |class ReBarBar : Bar<StringBuilder>()
            |class Foo<out T : Comparable<*>, R : List<Bar<*>>>
            |
            |fun <T : Comparable<Any?>> Foo<T, *>.qux(): String = TODO()
            |fun <T : Comparable<*>> Foo<T, *>.qux(): String = TODO()
            |
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { module ->
                // DRI(//qux/Foo[TypeParam(bounds=[kotlin.Comparable[kotlin.Any?]]),*]#//)
                val expectedDRI = DRI(
                    "",
                    null,
                    Callable(
                        "qux", TypeConstructor(
                            "Foo", listOf(
                                TypeParam(
                                    listOf(
                                        TypeConstructor(
                                            "kotlin.Comparable", listOf(
                                                Nullable(TypeConstructor("kotlin.Any", emptyList()))
                                            )
                                        )
                                    )
                                ),
                                StarProjection
                            )
                        ),
                        emptyList()
                    )
                )

                val driCount = module
                    .asSequence()
                    .filterIsInstance<ContentPage>()
                    .sumBy { it.dri.count { dri -> dri == expectedDRI } }

                assertEquals(1, driCount)
            }
        }
    }
}
