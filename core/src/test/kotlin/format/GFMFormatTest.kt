package org.jetbrains.dokka.tests

import org.jetbrains.dokka.GFMFormatService
import org.jetbrains.dokka.KotlinLanguageService
import org.junit.Test

class GFMFormatTest {
    private val gfmService = GFMFormatService(InMemoryLocationService, KotlinLanguageService())

    @Test fun sample() {
        verifyGFMNodeByName("sample", "Foo")
    }

    private fun verifyGFMNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/gfm/$fileName.kt", ".md") { model, output ->
            gfmService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members.filter { it.name == name })
        }
    }
}
