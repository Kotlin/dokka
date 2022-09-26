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
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    @Test
    fun `companion object with only jvmField should not be rendered`() {
        `companion object not rendered for declaration`("@JvmField val jvmFieldProp: String = \"\"")
    }

    @Test
    fun `companion property with jvmField should be static`() {
        `companion property is belong to outer class and static`("@JvmField val jvmFieldProp: String = \"\"", "jvmFieldProp")
    }

    @Test
    fun `companion object with only const should not be rendered`() {
        `companion object not rendered for declaration`("const val constProp: Int = 0")
    }

    @Test
    fun `companion property with const should be static`() {
        `companion property is belong to outer class and static`("const val constProp: Int = 0", "constProp")
    }

    @Test
    fun `companion object with only lateinit not rendered`() {
        `companion object not rendered for declaration`("lateinit var lateInitProp: String")
    }

    @Test
    fun `companion property with lateinit should be static`() {
        `companion property is belong to outer class and static`("lateinit var lateInitProp: String", "lateInitProp")
    }

    @Test
    fun `companion object with only jvmStatic fun not rendered`() {
        `companion object not rendered for declaration`("@JvmStatic fun staticFun(): String = \"\"")
    }

    @Test
    fun `companion function with JvmStatic should be static`() {
        `companion function is belong to outer class and static`("@JvmStatic fun staticFun(): String = \"\"", "staticFun")
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
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

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
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

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
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

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
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

                assertCompanionRendered(parentClass)

                val functions = parentClass.companion?.functions

                assertNotNull(functions)
                assertEquals(1, functions.size)
                assertTrue(functions.any { it.name == "renderedFun" })
                assertTrue(functions.none { it.name == "staticFun" })
            }
        }
    }


    private fun `companion object not rendered for declaration`(declaration: String) {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       $declaration
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

                assertCompanionNotRendered(parentClass)
            }
        }
    }

    private fun `companion property is belong to outer class and static`(declaration: String, name: String) {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       $declaration
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

                val outerClassProperty = parentClass.properties.firstOrNull{ it.name == name}
                assertNotNull(outerClassProperty, "Outer class contains the companion property")
                assertIsStatic(outerClassProperty)
            }
        }
    }

    private fun `companion function is belong to outer class and static`(declaration: String, name: String) {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    companion object $COMPANION_NAME {
            |       $declaration
            |    }
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val parentClass = module.packages.flatMap { it.classlikes }
                    .firstOrNull { it.name == "MyClass" } as DClass

                val outerClassFunction = parentClass.functions.firstOrNull{ it.name == name}
                assertNotNull(outerClassFunction, "Outer class contains the companion function")
                assertIsStatic(outerClassFunction)
            }
        }
    }
}

private fun assertCompanionRendered(parentClass: DClass){
    assertNotNull(parentClass.companion, "Companion should not be null")
    assertTrue(parentClass.classlikes.any { it.name == COMPANION_NAME },
        "Companion should be in classlikes list")
}

private fun assertCompanionNotRendered(parentClass: DClass){
    assertNull(parentClass.companion, "Companion should be null")
    assertTrue(parentClass.classlikes.none { it.name == COMPANION_NAME },
        "Companion should not be in classlikes list")
}

private fun assertIsStatic(property: DProperty){
    val extra = property.extra[AdditionalModifiers]
    assertNotNull(extra, "extra for property is present")
    assertTrue(extra.content.values.contains(setOf(ExtraModifiers.JavaOnlyModifiers.Static)),
        "Property contains extra modifier static")
}

private fun assertIsStatic(function: DFunction){
    val extra = function.extra[AdditionalModifiers]
    assertNotNull(extra, "extra for property is present")
    assertTrue(extra.content.values.contains(setOf(ExtraModifiers.JavaOnlyModifiers.Static)),
        "Function contains extra modifier static")
}