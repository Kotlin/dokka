package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.junit.Ignore
import org.junit.Test

@Ignore
class KotlinWebSiteFormatTest: FileGeneratorTestCase() {
    override val formatService = KotlinWebsiteFormatService(fileGenerator, KotlinLanguageService(), listOf(), DokkaConsoleLogger)

    @Test fun sample() {
        verifyKWSNodeByName("sample", "foo")
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
        verifyModelOutput(module, ".md", "testdata/format/website/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(
                    listOfNotNull(model.members.single().members.find { it.kind == NodeKind.GroupNode }),
                    output
            )
        }
        verifyMultiplatformPackage(module, path)
    }

    private fun verifyKWSNodeByName(fileName: String, name: String) {
        verifyOutput("testdata/format/website/$fileName.kt", ".md", ModelConfig(format = "kotlin-website")) { model, output ->
            buildPagesAndReadInto(
                    model.members.single().members.filter { it.name == name },
                    output
            )
        }
    }

    private fun buildMultiplePlatforms(path: String): DocumentationModule {
        val module = DocumentationModule("test")
        val passConfiguration = PassConfigurationImpl(noStdlibLink = true,
                noJdkLink = true,
                languageVersion = null,
                apiVersion = null
        )
        val configuration = DokkaConfigurationImpl(
            outputDir = "",
            format = "html",
            generateIndexPages = false,
            passesConfigurations = listOf(passConfiguration)
            )

        appendDocumentation(
            module, configuration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/website/$path/jvm.kt")),
                defaultPlatforms = listOf("JVM")
            )

        )


        appendDocumentation(
            module, configuration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/website/$path/jre7.kt")),
                defaultPlatforms = listOf("JVM", "JRE7")
            )
        )
        appendDocumentation(
            module, configuration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/website/$path/js.kt")),
                defaultPlatforms = listOf("JS")
            )
        )
        return module
    }

    private fun verifyMultiplatformPackage(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".package.md", "testdata/format/website/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
    }

}
