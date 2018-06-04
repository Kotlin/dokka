package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.junit.Before
import org.junit.Test

class KotlinWebSiteHtmlFormatTest: FileGeneratorTestCase() {
    override val formatService = KotlinWebsiteHtmlFormatService(fileGenerator, KotlinLanguageService(), listOf(), EmptyHtmlTemplateService)

    @Test fun dropImport() {
        verifyKWSNodeByName("dropImport", "foo")
    }

    @Test fun sample() {
        verifyKWSNodeByName("sample", "foo")
    }

    @Test fun sampleWithAsserts() {
        verifyKWSNodeByName("sampleWithAsserts", "a")
    }

    @Test fun newLinesInSamples() {
        verifyKWSNodeByName("newLinesInSamples", "foo")
    }

    @Test fun newLinesInImportList() {
        verifyKWSNodeByName("newLinesInImportList", "foo")
    }

    @Test fun returnTag() {
        verifyKWSNodeByName("returnTag", "indexOf")
    }

    @Test fun overloadGroup() {
        verifyKWSNodeByName("overloadGroup", "magic")
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

    private fun verifyKWSNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/website-html/$fileName.kt", ".html", format = "kotlin-website-html") { model, output ->
            buildPagesAndReadInto(model.members.single().members.filter { it.name == name }, output)
        }
    }

    private fun buildMultiplePlatforms(path: String): DocumentationModule {
        val module = DocumentationModule("test")
        val options = DocumentationOptions(
                outputDir = "",
                outputFormat = "kotlin-website-html",
                generateClassIndexPage = false,
                generatePackageIndexPage = false,
                noStdlibLink = true,
                noJdkLink = true,
                languageVersion = null,
                apiVersion = null
        )
        appendDocumentation(module, contentRootFromPath("testdata/format/website-html/$path/jvm.kt"), defaultPlatforms = listOf("JVM"), options = options)
        appendDocumentation(module, contentRootFromPath("testdata/format/website-html/$path/jre7.kt"), defaultPlatforms = listOf("JVM", "JRE7"), options = options)
        appendDocumentation(module, contentRootFromPath("testdata/format/website-html/$path/js.kt"), defaultPlatforms = listOf("JS"), options = options)
        return module
    }

    private fun verifyMultiplatformPackage(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".package.html", "testdata/format/website-html/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
    }

}
