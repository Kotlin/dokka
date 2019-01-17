package org.jetbrains.dokka.tests

import org.jetbrains.dokka.GLWFormatService
import org.jetbrains.dokka.KotlinLanguageService
import org.junit.Test

class GLWFormatTest : FileGeneratorTestCase() {

    override val formatService = GLWFormatService(fileGenerator, KotlinLanguageService(), listOf())

    @Test
    fun sample() {
        verifyGLWNodeByName("sample", "Foo")
    }

    @Test
    fun listInTableCell() {
        verifyGLWNodeByName("listInTableCell", "Foo")
    }

    private fun verifyGLWNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/glw/$fileName.kt", ".md") { model, output ->
            buildPagesAndReadInto(
                model.members.single().members.filter { it.name == name },
                output
            )
        }
    }
}