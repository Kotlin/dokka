package signatures

import org.junit.Test
import testApi.testRunner.AbstractCoreTest
import kotlin.test.assertEquals

class SignatureTest : AbstractCoreTest() {

    @Test
    fun signatureTest1() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/main/kotlin/signature/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/signature/Test.kt
            |package signature
            |
            |fun test(blk: (Int, String) -> String): String = blk(1, "")
            |fun String.test2(blk: String.() -> String): String = blk(this)
        """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = {
                println(it.dri)
                assertEquals(7, it.parentMap.size)
            }
        }
    }

}