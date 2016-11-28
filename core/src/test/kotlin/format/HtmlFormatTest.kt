package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.junit.Test
import java.io.File

class HtmlFormatTest {
    private val htmlService = HtmlFormatService(InMemoryLocationService, KotlinLanguageService(), HtmlTemplateService.default())

    @Test fun classWithCompanionObject() {
        verifyHtmlNode("classWithCompanionObject")
    }

    @Test fun htmlEscaping() {
        verifyHtmlNode("htmlEscaping")
    }

    @Test fun overloads() {
        verifyHtmlNodes("overloads") { model -> model.members }
    }

    @Test fun overloadsWithDescription() {
        verifyHtmlNode("overloadsWithDescription")
    }

    @Test fun overloadsWithDifferentDescriptions() {
        verifyHtmlNode("overloadsWithDifferentDescriptions")
    }

    @Test fun deprecated() {
        verifyOutput("testdata/format/deprecated.kt", ".package.html") { model, output ->
            htmlService.createOutputBuilder(output, tempLocation).appendNodes(model.members)
        }
        verifyOutput("testdata/format/deprecated.kt", ".class.html") { model, output ->
            htmlService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members)
        }
    }

    @Test fun brokenLink() {
        verifyHtmlNode("brokenLink")
    }

    @Test fun codeSpan() {
        verifyHtmlNode("codeSpan")
    }

    @Test fun parenthesis() {
        verifyHtmlNode("parenthesis")
    }

    @Test fun bracket() {
        verifyHtmlNode("bracket")
    }

    @Test fun see() {
        verifyHtmlNode("see")
    }

    @Test fun tripleBackticks() {
        verifyHtmlNode("tripleBackticks")
    }

    @Test fun typeLink() {
        verifyHtmlNodes("typeLink") { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun parameterAnchor() {
        verifyHtmlNode("parameterAnchor")
    }

    @Test fun javaSupertypeLink() {
        verifyJavaHtmlNodes("javaSupertype") { model ->
            model.members.single().members.single { it.name == "C" }.members.filter { it.name == "Bar" }
        }
    }

    @Test fun codeBlock() {
        verifyHtmlNode("codeBlock")
    }

    @Test fun javaLinkTag() {
        verifyJavaHtmlNode("javaLinkTag")
    }

    @Test fun javaLinkTagWithLabel() {
        verifyJavaHtmlNode("javaLinkTagWithLabel")
    }

    @Test fun javaSeeTag() {
        verifyJavaHtmlNode("javaSeeTag")
    }

    @Test fun javaDeprecated() {
        verifyJavaHtmlNodes("javaDeprecated") { model ->
            model.members.single().members.single { it.name == "Foo" }.members.filter { it.name == "foo" }
        }
    }

    @Test fun crossLanguageKotlinExtendsJava() {
        verifyOutput(arrayOf(KotlinSourceRoot("testdata/format/crossLanguage/kotlinExtendsJava/Bar.kt"),
                JavaSourceRoot(File("testdata/format/crossLanguage/kotlinExtendsJava"), null)),
                ".html") { model, output ->
            htmlService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun orderedList() {
        verifyHtmlNodes("orderedList") { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun linkWithLabel() {
        verifyHtmlNodes("linkWithLabel") { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun entity() {
        verifyHtmlNodes("entity") { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun uninterpretedEmphasisCharacters() {
        verifyHtmlNode("uninterpretedEmphasisCharacters")
    }

    @Test fun markdownInLinks() {
        verifyHtmlNode("markdownInLinks")
    }

    @Test fun returnWithLink() {
        verifyHtmlNode("returnWithLink")
    }

    @Test fun linkWithStarProjection() {
        verifyHtmlNode("linkWithStarProjection", withKotlinRuntime = true)
    }

    @Test fun functionalTypeWithNamedParameters() {
        verifyHtmlNode("functionalTypeWithNamedParameters")
    }

    private fun verifyHtmlNode(fileName: String, withKotlinRuntime: Boolean = false) {
        verifyHtmlNodes(fileName, withKotlinRuntime) { model -> model.members.single().members }
    }

    private fun verifyHtmlNodes(fileName: String,
                                withKotlinRuntime: Boolean = false,
                                nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyOutput("testdata/format/$fileName.kt", ".html", withKotlinRuntime = withKotlinRuntime) { model, output ->
            htmlService.createOutputBuilder(output, tempLocation).appendNodes(nodeFilter(model))
        }
    }

    private fun verifyJavaHtmlNode(fileName: String, withKotlinRuntime: Boolean = false) {
        verifyJavaHtmlNodes(fileName, withKotlinRuntime) { model -> model.members.single().members }
    }

    private fun verifyJavaHtmlNodes(fileName: String,
                                    withKotlinRuntime: Boolean = false,
                                    nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyJavaOutput("testdata/format/$fileName.java", ".html", withKotlinRuntime = withKotlinRuntime) { model, output ->
            htmlService.createOutputBuilder(output, tempLocation).appendNodes(nodeFilter(model))
        }
    }
}

