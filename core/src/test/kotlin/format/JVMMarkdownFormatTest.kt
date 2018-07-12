package org.jetbrains.dokka.tests.format

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.contentRootFromPath
import org.jetbrains.dokka.tests.BaseMarkdownFormatTest
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.verifyOutput
import org.junit.Test

class JVMMarkdownFormatTest: BaseMarkdownFormatTest(Platform.jvm) {

    @Test
    fun enumRef() {
        verifyMarkdownNode("enumRef", defaultModelConfig)
    }

    @Test
    fun javaCodeLiteralTags() {
        verifyJavaMarkdownNode("javaCodeLiteralTags", defaultModelConfig)
    }

    @Test
    fun nullability() {
        verifyMarkdownNode("nullability", defaultModelConfig)
    }

    @Test
    fun exceptionClass() {
        verifyMarkdownNode(
            "exceptionClass", ModelConfig(
                analysisPlatform = analysisPlatform,
                withKotlinRuntime = true
            )
        )
        verifyMarkdownPackage(
            "exceptionClass", ModelConfig(
                analysisPlatform = analysisPlatform,
                withKotlinRuntime = true
            )
        )
    }

    @Test
    fun operatorOverloading() {
        verifyMarkdownNodes("operatorOverloading", defaultModelConfig) { model->
            model.members.single().members.single { it.name == "C" }.members.filter { it.name == "plus" }
        }
    }

    @Test
    fun extensions() {
        verifyOutput("testdata/format/extensions.kt", ".package.md", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
        verifyOutput("testdata/format/extensions.kt", ".class.md", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members.single().members, output)
        }
    }

    @Test
    fun summarizeSignaturesProperty() {
        verifyMarkdownNodes("summarizeSignaturesProperty", defaultModelConfig) { model -> model.members }
    }

    @Test
    fun javaSpaceInAuthor() {
        verifyJavaMarkdownNode("javaSpaceInAuthor", defaultModelConfig)
    }

    @Test
    fun javaCodeInParam() {
        verifyJavaMarkdownNode("javaCodeInParam", defaultModelConfig)
    }

    @Test
    fun annotationParams() {
        verifyMarkdownNode("annotationParams", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun inheritedLink() {
        val filePath = "testdata/format/inheritedLink"
        verifyOutput(
            filePath,
            ".md",
            ModelConfig(
                roots = arrayOf(
                    contentRootFromPath("$filePath.kt"),
                    contentRootFromPath("$filePath.1.kt")
                ),
                withJdk = true,
                withKotlinRuntime = true,
                includeNonPublic = false,
                analysisPlatform = analysisPlatform

            )
        ) { model, output ->
            buildPagesAndReadInto(model.members.single { it.name == "p2" }.members.single().members, output)
        }
    }

    @Test
    fun javadocOrderedList() {
        verifyJavaMarkdownNodes("javadocOrderedList", defaultModelConfig) { model ->
            model.members.single().members.filter { it.name == "Bar" }
        }
    }

    @Test
    fun jdkLinks() {
        verifyMarkdownNode("jdkLinks", ModelConfig(withKotlinRuntime = true, analysisPlatform = analysisPlatform))
    }

    @Test
    fun javadocHtml() {
        verifyJavaMarkdownNode("javadocHtml", defaultModelConfig)
    }
}
