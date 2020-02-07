package signatures

import org.jetbrains.dokka.pages.*
import org.junit.Test
import testApi.testRunner.AbstractCoreTest

class SignatureTest : AbstractCoreTest() {

    @Test
    fun signatureTest1() {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/main/kotlin/signature/Test.kt")
                    targets = listOf("jvm", "js")
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
            pagesTransformationStage = { root ->
                val funs = root.children.first()
                    .children.mapNotNull { it as? MemberPageNode }.mapNotNull { it.content as? ContentGroup }
                    .mapNotNull { it.children.mapNotNull { it as? ContentGroup }.firstOrNull() }
                    .map { it.dci.dri.first().callable!!.name to it }.toMap()

                assert(funs.size == 2) { "Package page should contain 2 functions" }

                funs["test"].let { f ->
                    val expected = "public fun test(blk: (Int, String) -> String): String"
                    val obtained = f?.children?.join()
                    assert(expected == obtained) { "Expected: $expected\nObtained: $obtained" }
                }

                funs["test2"].let { f ->
                    val expected = "public fun String.test2(blk: String.() -> String): String"
                    val obtained = f?.children?.join()
                    assert(expected == obtained) { "Expected: $expected\nObtained: $obtained" }
                }

            }
        }
    }

    fun List<ContentNode>.join(): String = this.mapNotNull {
        when (val n = it) {
            is ContentText -> n.text
            is ContentDRILink -> n.children.join()
            else -> null
        }
    }.joinToString(separator = "")

}