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

    @Test
    fun codeBlocks() {
        verifyNode("codeBlocks.kt") { model ->
            listOf(model.members.single().members.single { it.name == "foo" })
        }
    }
}