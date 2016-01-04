package org.jetbrains.dokka.tests

import org.jetbrains.dokka.HtmlFormatService
import org.jetbrains.dokka.HtmlTemplateService
import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.junit.Test
import java.io.File

public class HtmlFormatTest {
    private val htmlService = HtmlFormatService(InMemoryLocationService, KotlinLanguageService(), HtmlTemplateService.default())

    @Test fun classWithCompanionObject() {
        verifyOutput("testdata/format/classWithCompanionObject.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun htmlEscaping() {
        verifyOutput("testdata/format/htmlEscaping.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun overloads() {
        verifyOutput("testdata/format/overloads.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members)
        }
    }

    @Test fun overloadsWithDescription() {
        verifyOutput("testdata/format/overloadsWithDescription.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun overloadsWithDifferentDescriptions() {
        verifyOutput("testdata/format/overloadsWithDifferentDescriptions.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun deprecated() {
        verifyOutput("testdata/format/deprecated.kt", ".package.html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members)
        }
        verifyOutput("testdata/format/deprecated.kt", ".class.html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun brokenLink() {
        verifyOutput("testdata/format/brokenLink.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun codeSpan() {
        verifyOutput("testdata/format/codeSpan.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun parenthesis() {
        verifyOutput("testdata/format/parenthesis.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun bracket() {
        verifyOutput("testdata/format/bracket.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun see() {
        verifyOutput("testdata/format/see.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun tripleBackticks() {
        verifyOutput("testdata/format/tripleBackticks.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeLink() {
        verifyOutput("testdata/format/typeLink.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar"} )
        }
    }

    @Test fun parameterAnchor() {
        verifyOutput("testdata/format/parameterAnchor.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaSupertypeLink() {
        verifyJavaOutput("testdata/format/javaSupertype.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.single { it.name == "C"}.members.filter { it.name == "Bar"} )
        }
    }

    @Test fun javaLinkTag() {
        verifyJavaOutput("testdata/format/javaLinkTag.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaLinkTagWithLabel() {
        verifyJavaOutput("testdata/format/javaLinkTagWithLabel.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaSeeTag() {
        verifyJavaOutput("testdata/format/javaSeeTag.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaDeprecated() {
        verifyJavaOutput("testdata/format/javaDeprecated.java", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.single { it.name == "Foo" }.members.filter { it.name == "foo" })
        }
    }

    @Test fun crossLanguageKotlinExtendsJava() {
        verifyOutput(arrayOf(KotlinSourceRoot("testdata/format/crossLanguage/kotlinExtendsJava/Bar.kt"),
                            JavaSourceRoot(File("testdata/format/crossLanguage/kotlinExtendsJava"), null)),
                ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun orderedList() {
        verifyOutput("testdata/format/orderedList.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun linkWithLabel() {
        verifyOutput("testdata/format/linkWithLabel.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun entity() {
        verifyOutput("testdata/format/entity.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun uninterpretedEmphasisCharacters() {
        verifyOutput("testdata/format/uninterpretedEmphasisCharacters.kt", ".html") { model, output ->
            htmlService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }
}

