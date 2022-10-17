package kotlinAsJavaPlugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val COMPANION_NAME = "C"

class CompanionAsJavaTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `empty companion object should not be rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {}
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion object with only jvmField should not be rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmField val jvmFieldProp: String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion property with jvmField should be static`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmField val jvmFieldProp: String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                val parentClassProperty = parentClass.properties.firstOrNull { it.name == "jvmFieldProp" }
                assertNotNull(parentClassProperty, "Parent class should contain the companion jvmField property")
                assertIsStatic(parentClassProperty)
            }
        }
    }

    @Test
    fun `companion object with only const should not be rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       const val constProp: Int = 0
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion property with const should be static`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       const val constProp: Int = 0
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                val parentClassProperty = parentClass.properties.firstOrNull { it.name == "constProp" }
                assertNotNull(parentClassProperty, "Parent class should contain the companion const property")
                assertIsStatic(parentClassProperty)
            }
        }
    }

    @Test
    fun `companion object with only lateinit not rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       lateinit var lateInitProp: String
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion property with lateinit should be static`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       lateinit var lateInitProp: String
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                val parentClassProperty = parentClass.properties.firstOrNull { it.name == "lateInitProp" }
                assertNotNull(parentClassProperty, "Parent class should contain the companion lateinit property")
                assertIsStatic(parentClassProperty)
            }
        }
    }

    @Test
    fun `companion object with only jvmStatic fun not rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmStatic fun staticFun(): String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion function with JvmStatic should be static`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmStatic fun staticFun(): String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                val parentClassFunction = parentClass.functions.firstOrNull { it.name == "staticFun" }
                assertNotNull(parentClassFunction, "Parent class should contains the companion jvmStatic function")
                assertIsStatic(parentClassFunction)
            }
        }
    }

    @Test
    fun `companion object with nested classes is rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmStatic
            |       fun staticFun1(): String = ""
            |       
            |       const val CONST_VAL: Int = 100
            |       
            |       class NestedClass
            |       object NestedObject
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionRendered(parentClass)

                val classLikes = parentClass.companion?.classlikes
                assertNotNull(classLikes)
                assertEquals(2, classLikes.size,
                    "Classlike list should contains nested class and object")
                assertTrue(classLikes.any { it.name == "NestedClass" })
                assertTrue(classLikes.any { it.name == "NestedObject" })

            }
        }
    }

    @Test
    fun `companion object with supertype is rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |
            |class Parent
            |interface IParent
            |class MyClass {
            |    companion object $COMPANION_NAME : Parent(), IParent {
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion object rendered for own properties`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmField
            |       val jvmField: String = ""
            |       const val contVal: Int = 0
            |       lateinit var lateInit: String
            |       
            |       val rendered: Int = TODO()
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionRendered(parentClass)

                val properties = parentClass.companion?.properties

                assertNotNull(properties)
                assertEquals(2, properties.size) // including INSTANCE
                assertTrue(properties.any { it.name == "rendered" })
                assertTrue(properties.none { it.name == "jvmField1" })
                assertTrue(properties.none { it.name == "contVal" })
                assertTrue(properties.none { it.name == "lateInit" })
            }
        }
    }

    @Test
    fun `companion object rendered for own functions`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       @JvmStatic
            |       fun staticFun(): String = ""
            |       
            |       fun renderedFun(): String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertCompanionRendered(parentClass)

                val functions = parentClass.companion?.functions

                assertNotNull(functions)
                assertEquals(1, functions.size)
                assertTrue(functions.any { it.name == "renderedFun" })
                assertTrue(functions.none { it.name == "staticFun" })
            }
        }
    }

    @Test
    fun `companion const value should be rendered as public by default`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       const val constProp: String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertEquals(
                    JavaVisibility.Public,
                    parentClass.properties.firstOrNull()?.visibility?.values?.first()
                )
                assertNull(parentClass.findFunction("constProp"), "There is no getter for the cont field")
            }
        }
    }

    @Test
    fun `companion const value should preserve Java modifier`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       protected const val constProp: String = ""
            |    }
            |}
        """.trimMargin(),
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        classpath += jvmStdlibPath!!
                        documentedVisibilities = setOf(
                            org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                            org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
                        )
                    }
                }
            },
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertEquals(
                    JavaVisibility.Protected,
                    parentClass.properties.firstOrNull()?.visibility?.values?.first()
                )
                assertNull(parentClass.findFunction("constProp"), "There is no getter for the cont field")
            }
        }
    }

    @Test
    fun `companion lateinit value should be rendered as public by default`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       lateinit var lateInitProp: String
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertEquals(
                    JavaVisibility.Public,
                    parentClass.properties.firstOrNull()?.visibility?.values?.first()
                )
                assertNull(parentClass.findFunction("lateInitProp"), "There is no getter for the cont field")
            }
        }
    }

    @Test
    fun `named companion instance property should be rendered if companion rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       var property: String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertNotNull(parentClass.properties.any { it.name == COMPANION_NAME })
            }
        }
    }

    @Test
    fun `default named companion instance property should be rendered if companion rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object {
            |       var property: String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertTrue(parentClass.properties.any { it.name == "Companion" })
            }
        }
    }

    @Test
    fun `companion instance property should be hidden if companion not rendered`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       const val property: String = ""
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.findClass("MyClass")

                assertTrue(parentClass.properties.none { it.name == COMPANION_NAME })
            }
        }
    }
}

private fun DModule.findClass(name: String) = packages.flatMap { it.classlikes }
    .firstOrNull { it.name == name } as DClass

private fun DClass.findFunction(name: String) = functions.firstOrNull { it.name.contains(name, ignoreCase = true) }

private fun assertCompanionRendered(parentClass: DClass) {
    assertNotNull(parentClass.companion, "Companion should not be null")
    assertTrue(
        parentClass.classlikes.any { it.name == COMPANION_NAME },
        "Companion should be in classlikes list"
    )
}

private fun assertCompanionNotRendered(parentClass: DClass) {
    assertNull(parentClass.companion, "Companion should be null")
    assertTrue(
        parentClass.classlikes.none { it.name == COMPANION_NAME },
        "Companion should not be in classlikes list"
    )
}

private fun assertIsStatic(property: DProperty) {
    val extra = property.extra[AdditionalModifiers]
    assertNotNull(extra, "extra for property is present")
    assertTrue(
        extra.content.values.contains(setOf(ExtraModifiers.JavaOnlyModifiers.Static)),
        "Property contains extra modifier static"
    )
}

private fun assertIsStatic(function: DFunction) {
    val extra = function.extra[AdditionalModifiers]
    assertNotNull(extra, "extra for property is present")
    assertTrue(
        extra.content.values.contains(setOf(ExtraModifiers.JavaOnlyModifiers.Static)),
        "Function contains extra modifier static"
    )
}