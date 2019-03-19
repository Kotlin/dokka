package org.jetbrains.dokka.tests.model

import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class SourceLinksTest(
    private val srcLink: String,
    private val url: String,
    private val lineSuffix: String?,
    private val expectedUrl: String
) {

    @Test
    fun test() {
        val link = if(srcLink.contains(sourceLinks)){
            srcLink.substringBeforeLast(sourceLinks) + sourceLinks
        } else {
            srcLink.substringBeforeLast(testdata) + testdata
        }
        val sourceLink = SourceLinkDefinitionImpl(link, url, lineSuffix)

        checkSourceExistsAndVerifyModel(filePath, ModelConfig(sourceLinks = listOf(sourceLink))) { model ->
            with(model.members.single().members.single()) {
                Assert.assertEquals("foo", name)
                Assert.assertEquals(NodeKind.Function, kind)
                Assert.assertEquals(expectedUrl, details(NodeKind.SourceUrl).single().name)
            }
        }
    }

    companion object {
        private const val testdata = "testdata"
        private const val sourceLinks = "sourceLinks"
        private const val dummy = "dummy.kt"
        private const val pathSuffix = "$sourceLinks/$dummy"
        private const val filePath = "$testdata/$pathSuffix"
        private const val url = "https://example.com"

        @Parameterized.Parameters(name = "{index}: {0}, {1}, {2} = {3}")
        @JvmStatic
        fun data(): Collection<Array<String?>> {
            val longestPath = File(testdata).absolutePath.removeSuffix("/") + "/../$testdata/"
            val maxLength = longestPath.length
            val list = listOf(
                arrayOf(File(testdata).absolutePath.removeSuffix("/"), "$url/$pathSuffix"),
                arrayOf(File("$testdata/$sourceLinks").absolutePath.removeSuffix("/") + "/", "$url/$dummy"),
                arrayOf(longestPath, "$url/$pathSuffix"),

                arrayOf(testdata, "$url/$pathSuffix"),
                arrayOf("./$testdata", "$url/$pathSuffix"),
                arrayOf("../core/$testdata", "$url/$pathSuffix"),
                arrayOf("$testdata/$sourceLinks", "$url/$dummy"),
                arrayOf("./$testdata/../$testdata/$sourceLinks", "$url/$dummy")
            )

            return list.map { arrayOf(it[0].padEnd(maxLength, '_'), url, null, it[1]) } +
                    listOf(
                        // check that it also works if url ends with /
                        arrayOf((File(testdata).absolutePath.removeSuffix("/") + "/").padEnd(maxLength, '_'), "$url/", null, "$url/$pathSuffix"),
                        // check if line suffix work
                        arrayOf<String?>("../core/../core/./$testdata/$sourceLinks/".padEnd(maxLength, '_'), "$url/", "#L", "$url/$dummy#L4")
                    )
        }
    }

}

