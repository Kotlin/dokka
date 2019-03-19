package org.jetbrains.dokka.tests

import org.jetbrains.dokka.GFMFormatService
import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.dokka.Platform
import org.junit.Test

abstract class BaseGFMFormatTest(val analysisPlatform: Platform) : FileGeneratorTestCase() {
    override val formatService = GFMFormatService(fileGenerator, KotlinLanguageService(), listOf())
    private val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)


    @Test
    fun sample() {
        verifyGFMNodeByName("sample", "Foo", defaultModelConfig)
    }

    @Test
    fun listInTableCell() {
        verifyGFMNodeByName("listInTableCell", "Foo", defaultModelConfig)
    }

    private fun verifyGFMNodeByName(fileName: String, name: String, modelConfig: ModelConfig) {
        verifyOutput("testdata/format/gfm/$fileName.kt", ".md", modelConfig) { model, output ->
            buildPagesAndReadInto(
                    model.members.single().members.filter { it.name == name },
                    output
            )
        }
    }
}


class JsGFMFormatTest : BaseGFMFormatTest(Platform.js)
class JvmGFMFormatTest : BaseGFMFormatTest(Platform.jvm)
class CommonGFMFormatTest : BaseGFMFormatTest(Platform.common)