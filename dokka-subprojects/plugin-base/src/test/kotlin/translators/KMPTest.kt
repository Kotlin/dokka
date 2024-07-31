/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import utils.AbstractModelTest
import utils.assertNotNull
import utils.comments
import utils.docs
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class KMPTest : AbstractModelTest("/src/main/kotlin/comment/Test.kt", "comment") {

    /// copy-pasted UTILS
    private fun getResourceAbsolutePath(resourcePath: String): String {
        val resource = object {}.javaClass.classLoader.getResource(resourcePath)?.file
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return File(resource).absolutePath
    }

    val configuration = dokkaConfiguration {
        sourceSets {
           /* val common =  sourceSet {
                sourceRoots = listOf("src/common/kotlin")
                analysisPlatform = "common"
                name = "common"
            }*/
            sourceSet {
                sourceRoots = listOf("src/androidMain/kotlin")
                analysisPlatform = "jvm"
                name = "android-example"
                classpath = listOf(getResourceAbsolutePath("jars/jvmAndroidLib-jvm-copy.jar"))
                // dependentSourceSets = setOf(common.value.sourceSetID)
            }
            sourceSet {
                sourceRoots = listOf("src/jvmMain/kotlin")
                analysisPlatform = "jvm"
                name = "jvm-example"
                classpath = listOf(getResourceAbsolutePath("jars/jvmAndroidLib-jvm.jar"))
               // dependentSourceSets = setOf(common.value.sourceSetID)
            }

        }
    }

    @Test
    fun `unresolved Firebase in the android source set with the same renamed jar`() {
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
                assertTrue(parameters[0].type is UnresolvedBound) // <------- here
            }
            with((this / "example" / "jvm").cast<DFunction>()) {
                assertTrue(parameters[0].type is GenericTypeConstructor)
            }
        }
    }
}
