/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinAsJavaPlugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text
import kotlin.test.*

class PropertyTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    @Test
    fun `property accessors should inherit doc`(){
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |class MyClass {
            |    /**
            |    * Some doc
            |    */
            |    var property: String = TODO()
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first()
                val getter = classLike.functions.firstOrNull { it.name == "getProperty" }
                val setter = classLike.functions.firstOrNull { it.name == "setProperty" }
                assertNotNull(getter)
                assertNotNull(setter)

                assertEquals("Some doc", getter.docText)
                assertEquals("Some doc", setter.docText)
            }
        }

        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/sample.kt
            |package kotlinAsJavaPlugin
            |/**
            |* @property property Some doc
            |*/
            |class MyClass {
            |    var property: String = TODO()
            |}
        """.trimMargin(),
            configuration,
        ) {
            documentablesTransformationStage = { module ->
                val classLike = module.packages.flatMap { it.classlikes }.first()
                val getter = classLike.functions.firstOrNull { it.name == "getProperty" }
                val setter = classLike.functions.firstOrNull { it.name == "setProperty" }
                assertNotNull(getter)
                assertNotNull(setter)

                assertEquals("Some doc", getter.docText)
                assertEquals("Some doc", setter.docText)
            }
        }
    }
}

private val Documentable.docText: String
    get() = this.documentation.values
        .flatMap { it.children }
        .map { it.root }
        .joinToString("") { it.text }

private val DocTag.text: String
    get() = if (this is Text) body else children.joinToString("") { it.text }
