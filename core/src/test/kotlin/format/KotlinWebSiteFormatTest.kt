package org.jetbrains.dokka.tests

import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.dokka.KotlinWebsiteFormatService
import org.junit.Test

class KotlinWebSiteFormatTest {
    private val kwsService = KotlinWebsiteFormatService(InMemoryLocationService, KotlinLanguageService())

    @Test fun sample() {
        verifyKWSNodeByName("sample", "foo")
    }

    private fun verifyKWSNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/website/$fileName.kt", ".md") { model, output ->
            kwsService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == name })
        }
    }
}
