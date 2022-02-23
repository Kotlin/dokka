package kotlinAsJavaPlugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.JavaVisibility
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JvmFieldTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `should keep properties annotated with JvmField as properties`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class SampleClass(@JvmField val property: String, val otherProperty: String)
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first()
                assertNotNull(classLike.properties.firstOrNull { it.name == "property" })
                assertEquals(
                    listOf("getOtherProperty"),
                    classLike.functions.map { it.name })
            }
        }
    }

    @Test
    fun `should work for top-level property`(){
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |@JvmField
            |val property: String = TODO()
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first()
                assertNotNull(classLike.properties.firstOrNull { it.name == "property" })
                assertEquals(
                    emptyList(),
                    classLike.functions.map { it.name })
            }
        }
    }

    @Test
    fun `properties without JvmName should be kept private`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class SampleClass(val property: String)
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first()
                assertEquals(JavaVisibility.Private, classLike.properties.firstOrNull()?.visibility?.values?.first())
                assertNotNull(classLike.functions.firstOrNull { it.name.startsWith("get") })
                assertEquals(
                    JavaVisibility.Public,
                    classLike.functions.first { it.name.startsWith("get") }.visibility.values.first()
                )
            }
        }
    }

    @Test
    fun `object jvmfield property should have no getters`(){
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |object MyObject {
            |    @JvmField
            |    val property: String = TODO()
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first()
                val property = classLike.properties.singleOrNull { it.name == "property" }
                assertNotNull(property)
                assertEquals(
                    emptyList(),
                    classLike.functions.map { it.name }
                )
                assertNull(property.getter)
                assertNull(property.setter)
            }
        }
    }
}