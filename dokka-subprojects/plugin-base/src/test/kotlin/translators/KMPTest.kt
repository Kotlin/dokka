/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import utils.AbstractModelTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KMPTest : AbstractModelTest("/src/main/kotlin/kmp/Test.kt", "kmp") {

    // copy-pasted org.jetbrains.dokka.analysis.test.api.util.getResourceAbsolutePath
    private fun getResourceAbsolutePath(resourcePath: String): String {
        val resource = object {}.javaClass.classLoader.getResource(resourcePath)?.file
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return File(resource).absolutePath
    }

    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/androidMain/kotlin")
                analysisPlatform = "jvm"
                name = "android-example"
                // contains only `class Firebase`
                classpath = listOf(getResourceAbsolutePath("jars/jvmAndroidLib-jvm-copy.jar"))
            }
            sourceSet {
                sourceRoots = listOf("src/jvmMain/kotlin")
                analysisPlatform = "jvm"
                name = "jvm-example"
                // contains only `class Firebase`
                classpath = listOf(getResourceAbsolutePath("jars/jvmAndroidLib-jvm.jar"))
            }

        }
    }

    @Test
    fun `should resolve Firebase from the same renamed jars #3702`() {
        inlineModelTest(
            """
                |/src/androidMain/kotlin/main.kt
                |package example
                |import Firebase
                |
                |fun android(f: Firebase){}   
                |             
                |/src/jvmMain/kotlin/main.kt
                |package example
                |import Firebase
                |
                |fun jvm(f: Firebase){}
            """,
            configuration = configuration
        ) {
            with((this / "example" / "android").cast<DFunction>()) {
                assertTrue(parameters[0].type is GenericTypeConstructor)
            }
            with((this / "example" / "jvm").cast<DFunction>()) {
                assertTrue(parameters[0].type is GenericTypeConstructor)
            }
        }
    }

    @Test
    fun `should resolve a class from a transitive common dependency`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val commonId =  sourceSet {
                    sourceRoots = listOf("src/c1/kotlin")
                    analysisPlatform = "common"
                    name = "c1"
                    classpath = listOf(getResourceAbsolutePath("jars/org.jetbrains.kotlin-kotlin-stdlib-2.2.0-commonMain-3ud7Cw.klib"))
                }.value.sourceSetID
                sourceSet {
                    sourceRoots = listOf("src/c2/kotlin")
                    analysisPlatform = "common"
                    name = "c2"
                    //classpath = listOf(getResourceAbsolutePath("jars/org.jetbrains.kotlin-kotlin-stdlib-2.2.0-commonMain-3ud7Cw.klib"))
                    dependentSourceSets = setOf(commonId)
                }

            }
        }
        inlineModelTest(
            """
                |/src/c1/kotlin/main.kt
                |package example
                |
                |public interface RawSink : AutoCloseable
                |             
                |/src/c2/kotlin/main.kt
                |package example
                |
                |public interface RawSink2 : AutoCloseable
            """,
            configuration = configuration
        ) {
            with((this / "example" / "RawSink").cast<DInterface>()) {
                assertEquals(DRI("kotlin", "AutoCloseable"), extra[ImplementedInterfaces]?.interfaces?.values?.firstOrNull()?.firstOrNull()?.dri)
            }
            with((this / "example" / "RawSink2").cast<DInterface>()) {
                // unresolved in K2
                assertEquals(DRI("kotlin", "AutoCloseable"), extra[ImplementedInterfaces]?.interfaces?.values?.firstOrNull()?.firstOrNull()?.dri)
            }
        }
    }
}
