package org.jetbrains.dokka.tests

import org.jetbrains.dokka.KotlinLanguageService
import org.junit.Test
import org.jetbrains.dokka.HtmlFormatService

public class HtmlFormatTest {
    private val htmlService = HtmlFormatService(InMemoryLocationService, KotlinLanguageService())

    Test fun classWithClassObject() {
        verifyOutput("test/data/format/classWithClassObject.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }
}
