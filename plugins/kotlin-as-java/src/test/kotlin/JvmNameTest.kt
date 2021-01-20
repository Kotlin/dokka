package kotlinAsJavaPlugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.isJvmName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmNameTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `should change name for class containing top level function`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |@file:JvmName("CustomJvmName")
            |package kotlinAsJavaPlugin
            |fun sample(): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val expectedClassLikeDri = DRI(
                    packageName = "kotlinAsJavaPlugin",
                    classNames = "CustomJvmName",
                )
                val classLike = module.packages.flatMap { it.classlikes }.first()
                assertEquals(expectedClassLikeDri, classLike.dri)
                assertEquals("CustomJvmName", classLike.name)
            }
        }
    }

    @Test
    fun `should change name for top level function`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |@file:JvmName("CustomJvmName")
            |package kotlinAsJavaPlugin
            |@JvmName("jvmSample")
            |fun sample(): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val expectedFunctionDri = DRI(
                    packageName = "kotlinAsJavaPlugin",
                    classNames = "CustomJvmName",
                    callable = Callable(
                        "jvmSample",
                        receiver = null,
                        params = emptyList()
                    )
                )
                val function = module.packages.flatMap { it.classlikes }.flatMap { it.functions }.first()
                assertEquals(expectedFunctionDri, function.dri)
                assertEquals("jvmSample", function.name)
            }
        }
    }

    @Test
    fun `should change name of a setter for top level property`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |@file:JvmName("CustomJvmName")
            |package kotlinAsJavaPlugin
            |@get:JvmName("xd")
            |@set:JvmName("asd")
            |var property: String
            |    get() = ""
            |    set(value) {}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val expectedSetterDri = DRI(
                    packageName = "kotlinAsJavaPlugin",
                    classNames = "CustomJvmName",
                    callable = Callable(
                        "asd",
                        receiver = null,
                        //Todo this is bad, this should be a type in java, look at the bytecode
                        params = listOf(TypeConstructor("kotlin.String", emptyList()))
                    )
                )
                val function =
                    module.packages.flatMap { it.classlikes }.flatMap { it.functions }.first { it.name == "asd" }
                assertEquals(expectedSetterDri, function.dri)
                assertEquals("asd", function.name)
            }
        }
    }

    @Test
    fun `should change name of a getter for top level property`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |@file:JvmName("CustomJvmName")
            |package kotlinAsJavaPlugin
            |@get:JvmName("xd")
            |@set:JvmName("asd")
            |var property: String
            |    get() = ""
            |    set(value) {}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val expectedGetterDri = DRI(
                    packageName = "kotlinAsJavaPlugin",
                    classNames = "CustomJvmName",
                    callable = Callable(
                        "xd",
                        receiver = null,
                        params = emptyList()
                    )
                )
                val function =
                    module.packages.flatMap { it.classlikes }.flatMap { it.functions }.first { it.name == "xd" }
                assertEquals(expectedGetterDri, function.dri)
                assertEquals("xd", function.name)
            }
        }
    }

    @Test
    fun `should leave the name as default if annotation is not provided`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |fun sample(): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val expectedClassLikeDri = DRI(
                    packageName = "kotlinAsJavaPlugin",
                    classNames = "SampleKt",
                )
                val classLike = module.packages.flatMap { it.classlikes }.first()
                assertEquals(expectedClassLikeDri, classLike.dri)
                assertEquals("SampleKt", classLike.name)
            }
        }
    }

    @Test
    fun `jvmName extra should be removed after the name swap`() {
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |@JvmName("CustomJvmName")
            |fun sample(): String = ""
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first() as DClass
                assertNull(
                    classLike.extra[Annotations]?.directAnnotations?.flatMap { it.value }
                        ?.map { it.dri }
                        ?.firstOrNull { it.isJvmName() }
                )
            }
        }
    }
}