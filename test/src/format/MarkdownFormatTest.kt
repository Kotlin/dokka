package org.jetbrains.dokka.tests

import org.junit.Test
import org.jetbrains.dokka.*
import java.io.File
import kotlin.test.assertEquals

public class MarkdownFormatTest {
    private val markdownService = MarkdownFormatService(InMemoryLocationService, KotlinLanguageService())

    Test fun emptyDescription() {
        verifyOutput("test/data/format/emptyDescription.kt") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun classWithClassObject() {
        verifyOutput("test/data/format/classWithClassObject.kt") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }
}
