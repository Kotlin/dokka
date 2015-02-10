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

    Test fun htmlEscaping() {
        verifyOutput("test/data/format/htmlEscaping.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun overloads() {
        verifyOutput("test/data/format/overloads.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members)
        }
    }

    Test fun deprecated() {
        verifyOutput("test/data/format/deprecated.kt", ".package.html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members)
        }
        verifyOutput("test/data/format/deprecated.kt", ".class.html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun brokenLink() {
        verifyOutput("test/data/format/brokenLink.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun codeSpan() {
        verifyOutput("test/data/format/codeSpan.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun parenthesis() {
        verifyOutput("test/data/format/parenthesis.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun see() {
        verifyOutput("test/data/format/see.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun tripleBackticks() {
        verifyOutput("test/data/format/tripleBackticks.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }
}
