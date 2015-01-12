package org.jetbrains.dokka.tests

import org.junit.Test
import org.jetbrains.dokka.*
import java.io.File
import kotlin.test.assertEquals

public class MarkdownFormatTest {
    private val markdownService = MarkdownFormatService(InMemoryLocationService, KotlinLanguageService())

    Test fun emptyDescription() {
        verifyOutput("test/data/format/emptyDescription.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun classWithClassObject() {
        verifyOutput("test/data/format/classWithClassObject.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun annotations() {
        verifyOutput("test/data/format/annotations.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun annotationClass() {
        verifyOutput("test/data/format/annotationClass.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun annotationParams() {
        verifyOutput("test/data/format/annotationParams.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun extensions() {
        verifyOutput("test/data/format/extensions.kt", ".package.md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
        verifyOutput("test/data/format/extensions.kt", ".class.md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun enumClass() {
        verifyOutput("test/data/format/enumClass.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }
}
