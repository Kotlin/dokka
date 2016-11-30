package org.jetbrains.dokka.tests

import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.dokka.KotlinWebsiteFormatService
import org.junit.Test

class KotlinWebSiteFormatTest {
    private val kwsService = KotlinWebsiteFormatService(InMemoryLocationService, KotlinLanguageService())

    @Test fun sample() {
        verifyKWSNodeByName("sample", "foo")
    }

    @Test fun returnTag() {
        verifyKWSNodeByName("returnTag", "indexOf")
    }

    @Test fun overloadGroup() {
        verifyKWSNodeByName("overloadGroup", "magic")
    }

    private fun verifyKWSNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/website/$fileName.kt", ".md", format = "kotlin-website") { model, output ->
            kwsService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members.filter { it.name == name })
        }
    }
}
