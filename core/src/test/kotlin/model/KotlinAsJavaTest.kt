package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationModule
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.RefKind
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinAsJavaTest {
    @Test fun function() {
        verifyModelAsJava("testdata/functions/function.kt") { model ->
            val pkg = model.members.single()

            val facadeClass = pkg.members.single { it.name == "FunctionKt" }
            assertEquals(NodeKind.Class, facadeClass.kind)

            val fn = facadeClass.members.single { it.kind == NodeKind.Function}
            assertEquals("fn", fn.name)
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


    @Test fun constants() {
        verifyModelAsJava("testdata/java/constants.java") { cls ->
            selectNodes(cls) {
                subgraphOf(RefKind.Member)
                matching { it.name == "constStr" || it.name == "refConst" }
            }.forEach {
                assertEquals("In $it", "\"some value\"", it.detailOrNull(NodeKind.Value)?.name)
            }
            val nullConstNode = selectNodes(cls) {
                subgraphOf(RefKind.Member)
                withName("nullConst")
            }.single()

            Assert.assertNull(nullConstNode.detailOrNull(NodeKind.Value))
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
