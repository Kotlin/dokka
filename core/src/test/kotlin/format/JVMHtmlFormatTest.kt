package org.jetbrains.dokka.tests.format

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.BaseHtmlFormatTest
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.verifyOutput
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.junit.Test
import java.io.File

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
                    KotlinSourceRoot("testdata/format/crossLanguage/kotlinExtendsJava/Bar.kt"),
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