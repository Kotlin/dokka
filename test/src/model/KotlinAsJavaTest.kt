package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationModule
import org.jetbrains.dokka.DocumentationNode
import org.junit.Test
import kotlin.test.assertEquals

class KotlinAsJavaTest {
    @Test fun function() {
        verifyModelAsJava("test/data/functions/function.kt") { model ->
            val pkg = model.members.single()

            val facadeClass = pkg.members.single { it.name == "FunctionKt" }
            assertEquals(DocumentationNode.Kind.Class, facadeClass.kind)

            val fn = facadeClass.members.single()
            assertEquals("fn", fn.name)
            assertEquals(DocumentationNode.Kind.Function, fn.kind)
        }
    }

    @Test fun propertyWithComment() {
        verifyModelAsJava("test/data/comments/oneLineDoc.kt") { model ->
            val facadeClass = model.members.single().members.single { it.name == "OneLineDocKt" }
            val getter = facadeClass.members.single { it.name == "getProperty" }
            assertEquals(DocumentationNode.Kind.Function, getter.kind)
            assertEquals("doc", getter.content.summary.toTestString())
        }
    }
}

fun verifyModelAsJava(source: String,
                      withJdk: Boolean = false,
                      withKotlinRuntime: Boolean = false,
                      verifier: (DocumentationModule) -> Unit) {
    verifyModel(source,
            withJdk = withJdk, withKotlinRuntime = withKotlinRuntime,
            format = "html-as-java",
            verifier = verifier)
}
