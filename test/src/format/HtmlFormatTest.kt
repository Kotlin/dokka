package org.jetbrains.dokka.tests

import org.jetbrains.dokka.KotlinLanguageService
import org.junit.Test
import org.jetbrains.dokka.HtmlFormatService

public class HtmlFormatTest {
    private val htmlService = HtmlFormatService(InMemoryLocationService, KotlinLanguageService())

    Test fun classWithDefaultObject() {
        verifyOutput("test/data/format/classWithDefaultObject.kt", ".html") { model, output ->
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

    Test fun overloadsWithDescription() {
        verifyOutput("test/data/format/overloadsWithDescription.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun overloadsWithDifferentDescriptions() {
        verifyOutput("test/data/format/overloadsWithDifferentDescriptions.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
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

    Test fun bracket() {
        verifyOutput("test/data/format/bracket.kt", ".html") { model, output ->
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

    Test fun typeLink() {
        verifyOutput("test/data/format/typeLink.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar"} )
        }
    }

    Test fun parameterAnchor() {
        verifyOutput("test/data/format/parameterAnchor.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun javaSupertypeLink() {
        verifyOutput("test/data/format/javaSupertype.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.single { it.name == "C"}.members.filter { it.name == "Bar"} )
        }
    }

    Test fun javaLinkTag() {
        verifyOutput("test/data/format/javaLinkTag.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun javaSeeTag() {
        verifyOutput("test/data/format/javaSeeTag.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    Test fun javaDeprecated() {
        verifyOutput("test/data/format/javaDeprecated.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.single { it.name == "Foo" }.members.filter { it.name == "foo" })
        }
    }

    Test fun crossLanguageKotlinExtendsJava() {
        verifyOutput("test/data/format/crossLanguage/kotlinExtendsJava", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    Test fun orderedList() {
        verifyOutput("test/data/format/orderedList.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }
}
