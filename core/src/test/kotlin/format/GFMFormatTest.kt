package org.jetbrains.dokka.tests

import org.jetbrains.dokka.GFMFormatService
import org.jetbrains.dokka.KotlinLanguageService
import org.junit.Before
import org.junit.Test

class GFMFormatTest {
    private val gfmService = GFMFormatService(TestFileGenerator, KotlinLanguageService(), listOf())

    @Before
    fun prepareFileGenerator() {
        TestFileGenerator.formatService = gfmService
    }

    @Test
    fun sample() {
        verifyGFMNodeByName("sample", "Foo")
    }

    @Test
    fun listInTableCell() {
        verifyGFMNodeByName("listInTableCell", "Foo")
    }

    private fun verifyGFMNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/gfm/$fileName.kt", ".md") { model, output ->
            TestFileGenerator.buildPagesAndReadInto(
                    model.members.single().members.filter { it.name == name },
                    output
            )
        }
    }
}
