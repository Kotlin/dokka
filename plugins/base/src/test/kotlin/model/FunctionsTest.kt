package model

import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.GenericType
import org.junit.Test

class FunctionsTest : AbstractModelTest() {

    // issue dokka#659
    @Test
    fun genericFunction() {
        inlineModelTest(
            """
                |/src/main/kotlin/function/Test.kt
                |package function
                | /**
                | * generic function
                | */
                |private fun <T> generic() = TODO()
        """
        ) {
            with((this / "function" / "generic").assertIsInstance<Function>("generic")) {
                name.assertEqual("generic", "Function name: ")
                visibility.values.first().assertEqual(org.jetbrains.kotlin.descriptors.Visibilities.PRIVATE)
                parameters.assertCount(0)
                returnType.assertNotNull("returnType").constructorFqName.assertEqual("", "Return type: ") // TODO ??
                typeParameters.assertCount(1, "Type parameters: ")
                typeParameters.single().name.assertEqual("T", "Type parameter name: ")
                typeParameters.single().bounds.assertCount(1, "Type parameter bounds: ")
            }
        }
    }

    @Test
    fun genericFunctionWithConstraints() {
        inlineModelTest(
            """
                |/src/main/kotlin/function/Test.kt
                |package function
                | /**
                | * generic function
                | */
                |public fun <T : R, R> generic() {}
                |class Test<T>() {}
        """
        ) {
            with((this / "function" / "generic").assertIsInstance<Function>("generic")) {
                name.assertEqual("generic", "Function name: ")
                visibility.values.first().assertEqual(org.jetbrains.kotlin.descriptors.Visibilities.PUBLIC)
                parameters.assertCount(0)
                returnType.assertNotNull("returnType").constructorFqName.assertEqual("kotlin.Unit", "Return type: ")

                typeParameters.assertCount(2, "Type parameters: ")
                val T = typeParameters.find { it.name == "T" }.assertIsInstance<GenericType>("T")
                val R = typeParameters.find { it.name == "R" }.assertIsInstance<GenericType>("R")

                with(T) {
                    name.assertEqual("T", "First type parameter name: ")
                    bounds.assertCount(1, "First type parameter bounds: ")
                }
                with(R) {
                    name.assertEqual("R", "Second type parameter name: ")
                    bounds.assertCount(1, "Second type parameter bounds: ")
                }
            }

        }
    }

    @Test
    fun comparableHell() {
        inlineModelTest(
            """
                |/src/main/kotlin/function/Test.kt
                |package comparable
                | /**
                | * generic function
                | */
                |public fun <T : Comparable<R>, R> generic() {}
                |class Test<T>() {}
        """
        ) {
            with((this / "comparable" / "generic").assertIsInstance<Function>("generic")) {
                name.assertEqual("generic", "Function name: ")
                visibility.values.first().assertEqual(org.jetbrains.kotlin.descriptors.Visibilities.PUBLIC)
                parameters.assertCount(0)
                returnType.assertNotNull("returnType").constructorFqName.assertEqual("kotlin.Unit", "Return type: ")

                typeParameters.assertCount(2, "Type parameters: ")
                val T = typeParameters.find { it.name == "T" }.assertIsInstance<GenericType>("T")
                val R = typeParameters.find { it.name == "R" }.assertIsInstance<GenericType>("R")

                with(T) {
                    name.assertEqual("T", "First type parameter name: ")
                    bounds.assertCount(1, "First type parameter bounds: ")
                    with(bounds.single().assertIsInstance<TypeConstructor>("Comparable")) {
                        name.assertEqual("kotlin.Comparable", "Comparable TypeConstructor: ")
                        bounds.assertCount(1, "Comparable bounds: ")
                        bounds.single().name.assertEqual("R", "Comparable bound: ")
                    }
                }
                with(R) {
                    name.assertEqual("R", "Second type parameter name: ")
                    bounds.assertCount(1, "Second type parameter bounds: ")
                }
            }

        }

    }

    @Test
    fun multipleGenerics() {
        inlineModelTest(
            """
                |/src/main/kotlin/function/Test.kt
                |package function
                | /**
                | * generic function
                | */
                |public fun <A, T : R, R : Z, Z : Iterable<A>> generic() {}
        """
        ) {
            with((this / "function" / "generic").assertIsInstance<Function>("generic")) {
                name.assertEqual("generic", "Function name: ")
                visibility.values.first().assertEqual(org.jetbrains.kotlin.descriptors.Visibilities.PUBLIC)
                parameters.assertCount(0)
                returnType.assertNotNull("returnType").constructorFqName.assertEqual("kotlin.Unit", "Return type: ")

                typeParameters.assertCount(4, "Type parameters: ")
                val A = typeParameters.find { it.name == "A" }.assertIsInstance<GenericType>("A")
                val T = typeParameters.find { it.name == "T" }.assertIsInstance<GenericType>("T")
                val R = typeParameters.find { it.name == "R" }.assertIsInstance<GenericType>("R")
                val Z = typeParameters.find { it.name == "Z" }.assertIsInstance<GenericType>("Z")

                with(A) {
                    name.assertEqual("A", "First type parameter name: ")
                    print().assertEqual("A: kotlin.Any<>")
                }
                with(T) {
                    name.assertEqual("T", "Second type parameter name: ")
                    print().assertEqual("T: R: Z: kotlin.collections.Iterable<A: kotlin.Any<>>")
                }
                with(R) {
                    name.assertEqual("R", "First type parameter name: ")
                    R.print().assertEqual("R: Z: kotlin.collections.Iterable<A: kotlin.Any<>>")
                }
                with(Z) {
                    name.assertEqual("Z", "Second type parameter name: ")
                    print().assertEqual("Z: kotlin.collections.Iterable<A: kotlin.Any<>>")
                }

                assert(T.bounds.single().assertIsInstance<GenericType.GenericReference>("GenericReference").ref === R)
                assert(R.bounds.single().assertIsInstance<GenericType.GenericReference>("GenericReference").ref === Z)
                assert(Z.bounds.single().bounds.single().assertIsInstance<GenericType.GenericReference>("GenericReference").ref === A)
            }

        }
    }

    @Test
    fun test1() {
        inlineModelTest(
            """
            |/src/main/kotlin/function/Test.kt
            |package function
            |
            |fun <T : Comparable<T>> Array<T>.test(t: T?): Array<T> = TODO()
            |}
        """
        ) {
            with((this / "function" / "test").assertIsInstance<Function>("test")) {
                val params = this.typeParameters
                params.single().print().assertEqual("T: kotlin.Comparable<(T)>", "Parameter: ")
            }
        }

    }
}