package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.junit.Test
import java.io.File

abstract class BaseHtmlFormatTest(val analysisPlatform: Platform): FileGeneratorTestCase() {
    protected val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    override val formatService = HtmlFormatService(fileGenerator, KotlinLanguageService(), HtmlTemplateService.default(), listOf())

    @Test fun classWithCompanionObject() {
        verifyHtmlNode("classWithCompanionObject", defaultModelConfig)
    }

    @Test fun htmlEscaping() {
        verifyHtmlNode("htmlEscaping", defaultModelConfig)
    }

    @Test fun overloads() {
        verifyHtmlNodes("overloads", defaultModelConfig) { model -> model.members }
    }

    @Test fun overloadsWithDescription() {
        verifyHtmlNode("overloadsWithDescription", defaultModelConfig)
    }

    @Test fun overloadsWithDifferentDescriptions() {
        verifyHtmlNode("overloadsWithDifferentDescriptions", defaultModelConfig)
    }

    @Test fun deprecated() {
        verifyOutput("testdata/format/deprecated.kt", ".package.html", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
        verifyOutput("testdata/format/deprecated.kt", ".class.html", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members.single().members, output)
        }
    }

    @Test fun brokenLink() {
        verifyHtmlNode("brokenLink", defaultModelConfig)
    }

    @Test fun codeSpan() {
        verifyHtmlNode("codeSpan", defaultModelConfig)
    }

    @Test fun parenthesis() {
        verifyHtmlNode("parenthesis", defaultModelConfig)
    }

    @Test fun bracket() {
        verifyHtmlNode("bracket", defaultModelConfig)
    }

    @Test fun see() {
        verifyHtmlNode("see", defaultModelConfig)
    }

    @Test fun tripleBackticks() {
        verifyHtmlNode("tripleBackticks", defaultModelConfig)
    }

    @Test fun typeLink() {
        verifyHtmlNodes("typeLink", defaultModelConfig) { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun parameterAnchor() {
        verifyHtmlNode("parameterAnchor", defaultModelConfig)
    }

    @Test fun codeBlock() {
        verifyHtmlNode("codeBlock", defaultModelConfig)
    }
    @Test fun orderedList() {
        verifyHtmlNodes("orderedList", defaultModelConfig) { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun linkWithLabel() {
        verifyHtmlNodes("linkWithLabel", defaultModelConfig) { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun entity() {
        verifyHtmlNodes("entity", defaultModelConfig) { model -> model.members.single().members.filter { it.name == "Bar" } }
    }

    @Test fun uninterpretedEmphasisCharacters() {
        verifyHtmlNode("uninterpretedEmphasisCharacters", defaultModelConfig)
    }

    @Test fun markdownInLinks() {
        verifyHtmlNode("markdownInLinks", defaultModelConfig)
    }

    @Test fun returnWithLink() {
        verifyHtmlNode("returnWithLink", defaultModelConfig)
    }

    @Test fun linkWithStarProjection() {
        verifyHtmlNode("linkWithStarProjection", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun functionalTypeWithNamedParameters() {
        verifyHtmlNode("functionalTypeWithNamedParameters", defaultModelConfig)
    }

    @Test fun sinceKotlin() {
        verifyHtmlNode("sinceKotlin", defaultModelConfig)
    }

    @Test fun blankLineInsideCodeBlock() {
        verifyHtmlNode("blankLineInsideCodeBlock", defaultModelConfig)
    }

    @Test fun indentedCodeBlock() {
        verifyHtmlNode("indentedCodeBlock", defaultModelConfig)
    }

    private fun verifyHtmlNode(fileName: String, modelConfig: ModelConfig = ModelConfig()) {
        verifyHtmlNodes(fileName, modelConfig) { model -> model.members.single().members }
    }

    private fun verifyHtmlNodes(fileName: String,
                                modelConfig: ModelConfig = ModelConfig(),
                                nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyOutput("testdata/format/$fileName.kt", ".html", modelConfig) { model, output ->
            buildPagesAndReadInto(nodeFilter(model), output)
        }
    }

    protected fun verifyJavaHtmlNode(fileName: String, modelConfig: ModelConfig = ModelConfig()) {
        verifyJavaHtmlNodes(fileName, modelConfig) { model -> model.members.single().members }
    }

    protected fun verifyJavaHtmlNodes(fileName: String,
                                      modelConfig: ModelConfig = ModelConfig(),
                                      nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyJavaOutput("testdata/format/$fileName.java", ".html", modelConfig) { model, output ->
            buildPagesAndReadInto(nodeFilter(model), output)
        }
    }
}

class JSHtmlFormatTest: BaseHtmlFormatTest(Platform.js)

class JVMHtmlFormatTest: BaseHtmlFormatTest(Platform.jvm) {
    @Test
    fun javaSeeTag() {
        verifyJavaHtmlNode("javaSeeTag", defaultModelConfig)
    }

    @Test fun javaDeprecated() {
        verifyJavaHtmlNodes("javaDeprecated", defaultModelConfig) { model ->
            model.members.single().members.single { it.name == "Foo" }.members.filter { it.name == "foo" }
        }
    }

    @Test fun crossLanguageKotlinExtendsJava() {
        verifyOutput(
            ModelConfig(
                roots = arrayOf(
                    KotlinSourceRoot("testdata/format/crossLanguage/kotlinExtendsJava/Bar.kt", false),
                    JavaSourceRoot(File("testdata/format/crossLanguage/kotlinExtendsJava"), null)
                ),
                analysisPlatform = analysisPlatform
            ), ".html") { model, output ->
            buildPagesAndReadInto(
                model.members.single().members.filter { it.name == "Bar" },
                output
            )
        }
    }

    @Test fun javaLinkTag() {
        verifyJavaHtmlNode("javaLinkTag", defaultModelConfig)
    }

    @Test fun javaLinkTagWithLabel() {
        verifyJavaHtmlNode("javaLinkTagWithLabel", defaultModelConfig)
    }

    @Test fun javaSupertypeLink() {
        verifyJavaHtmlNodes("JavaSupertype", defaultModelConfig) { model ->
            model.members.single().members.single { it.name == "JavaSupertype" }.members.filter { it.name == "Bar" }
        }
    }

}

class CommonHtmlFormatTest: BaseHtmlFormatTest(Platform.common)