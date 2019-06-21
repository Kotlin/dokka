package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.jetbrains.dokka.Generation.DocumentationMerger
import org.junit.Test

abstract class BaseKotlinWebSiteHtmlFormatTest(val analysisPlatform: Platform): FileGeneratorTestCase() {
    val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    override val formatService = KotlinWebsiteHtmlFormatService(fileGenerator, KotlinLanguageService(), listOf(), EmptyHtmlTemplateService)

    @Test fun dropImport() {
        verifyKWSNodeByName("dropImport", "foo", defaultModelConfig)
    }

    @Test fun sample() {
        verifyKWSNodeByName("sample", "foo", defaultModelConfig)
    }

    @Test fun sampleWithAsserts() {
        verifyKWSNodeByName("sampleWithAsserts", "a", defaultModelConfig)
    }

    @Test fun newLinesInSamples() {
        verifyKWSNodeByName("newLinesInSamples", "foo", defaultModelConfig)
    }

    @Test fun newLinesInImportList() {
        verifyKWSNodeByName("newLinesInImportList", "foo", defaultModelConfig)
    }

    @Test fun returnTag() {
        verifyKWSNodeByName("returnTag", "indexOf", defaultModelConfig)
    }

    @Test fun overloadGroup() {
        verifyKWSNodeByName("overloadGroup", "magic", defaultModelConfig)
    }

    @Test fun dataTags() {
        val module = buildMultiplePlatforms("dataTags")
        verifyMultiplatformPackage(module, "dataTags")
    }

    @Test fun dataTagsInGroupNode() {
        val path = "dataTagsInGroupNode"
        val module = buildMultiplePlatforms(path)
        verifyModelOutput(module, ".html", "testdata/format/website-html/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(
                    listOfNotNull(model.members.single().members.find { it.kind == NodeKind.GroupNode }),
                    output
            )
        }
        verifyMultiplatformPackage(module, path)
    }

    private fun verifyKWSNodeByName(fileName: String, name: String, modelConfig: ModelConfig) {
        verifyOutput(
            "testdata/format/website-html/$fileName.kt",
            ".html",
            ModelConfig(analysisPlatform = modelConfig.analysisPlatform, format = "kotlin-website-html")
        ) { model, output ->
            buildPagesAndReadInto(model.members.single().members.filter { it.name == name }, output)
        }
    }

    private fun buildMultiplePlatforms(path: String): DocumentationModule {
        val moduleName = "test"
        val passConfiguration = PassConfigurationImpl(
                noStdlibLink = true,
                noJdkLink = true,
                languageVersion = null,
                apiVersion = null
        )

        val dokkaConfiguration = DokkaConfigurationImpl(
            outputDir = "",
            format = "kotlin-website-html",
            generateIndexPages = false,
            passesConfigurations = listOf(
                passConfiguration
            )

        )

        val module1 = DocumentationModule(moduleName)
        appendDocumentation(
            module1, dokkaConfiguration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/website-html/$path/jvm.kt")),
                defaultPlatforms = listOf("JVM")
            )
        )

        val module2 = DocumentationModule(moduleName)
        appendDocumentation(
            module2, dokkaConfiguration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/website-html/$path/jre7.kt")),
                defaultPlatforms = listOf("JVM", "JRE7")
            )
        )

        val module3 = DocumentationModule(moduleName)
        appendDocumentation(
            module3, dokkaConfiguration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/website-html/$path/js.kt")),
                defaultPlatforms = listOf("JS")
            )
        )

        return DocumentationMerger(listOf(module1, module2, module3), DokkaConsoleLogger).merge()
    }

    private fun verifyMultiplatformPackage(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".package.html", "testdata/format/website-html/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
    }

}
class JsKotlinWebSiteHtmlFormatTest: BaseKotlinWebSiteHtmlFormatTest(Platform.js)

class JvmKotlinWebSiteHtmlFormatTest: BaseKotlinWebSiteHtmlFormatTest(Platform.jvm)

class CommonKotlinWebSiteHtmlFormatTest: BaseKotlinWebSiteHtmlFormatTest(Platform.common)