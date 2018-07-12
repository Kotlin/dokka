package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationModule
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.junit.Test
import org.junit.Assert.assertEquals

class KotlinAsJavaTest {
    @Test fun function() {
        verifyModelAsJava("testdata/functions/function.kt") { model ->
            val pkg = model.members.single()

            val facadeClass = pkg.members.single { it.name == "FunctionKt" }
            assertEquals(NodeKind.Class, facadeClass.kind)

            val fn = facadeClass.members.single()
            assertEquals("fn", fn.name)
            assertEquals(NodeKind.Function, fn.kind)
        }
    }

    @Test fun propertyWithComment() {
        verifyModelAsJava("testdata/comments/oneLineDoc.kt") { model ->
            val facadeClass = model.members.single().members.single { it.name == "OneLineDocKt" }
            val getter = facadeClass.members.single { it.name == "getProperty" }
            assertEquals(NodeKind.Function, getter.kind)
            assertEquals("doc", getter.content.summary.toTestString())
        }
    }
}

fun verifyModelAsJava(source: String,
                      modelConfig: ModelConfig = ModelConfig(),
                      verifier: (DocumentationModule) -> Unit) {
    checkSourceExistsAndVerifyModel(
        source,
        modelConfig = ModelConfig(
            withJdk = modelConfig.withJdk,
            withKotlinRuntime = modelConfig.withKotlinRuntime,
            format = "html-as-java",
            analysisPlatform = Platform.jvm),
        verifier = verifier
    )
}
