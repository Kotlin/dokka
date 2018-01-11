package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Formats.JavaLayoutHtmlFormatDescriptor
import org.junit.Test

class JavaLayoutHtmlFormatTest : JavaLayoutHtmlFormatTestCase() {
    override val formatDescriptor = JavaLayoutHtmlFormatDescriptor()

    @Test
    fun simple() {
        verifyNode("simple.kt")
    }

    @Test
    fun topLevel() {
        verifyPackageNode("topLevel.kt")
    }

    private fun verifyNode(fileName: String) {
        verifyOutput(
            "testdata/format/java-layout-html/$fileName",
            ".html",
            format = "java-layout-html",
            withKotlinRuntime = true,
            noStdlibLink = false
        ) { model, output ->
            buildPagesAndReadInto(
                model,
                listOf(model.members.single().members.single()),
                output
            )
        }
    }

    private fun verifyPackageNode(fileName: String) {
        verifyOutput(
            "testdata/format/java-layout-html/$fileName",
            ".package-summary.html",
            format = "java-layout-html"
        ) { model, output ->
            buildPagesAndReadInto(
                model,
                listOf(model.members.single()),
                output
            )
        }
    }
}