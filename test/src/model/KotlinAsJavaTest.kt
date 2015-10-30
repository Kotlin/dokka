package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationModule
import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.KotlinAsJavaDocumentationBuilder
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
            assertEquals(DocumentationNode.Kind.CompanionObjectFunction, fn.kind)
        }
    }
}

fun verifyModelAsJava(source: String,
                      withJdk: Boolean = false,
                      withKotlinRuntime: Boolean = false,
                      verifier: (DocumentationModule) -> Unit) {
    verifyModel(source,
            withJdk = withJdk, withKotlinRuntime = withKotlinRuntime,
            packageDocumentationBuilder = KotlinAsJavaDocumentationBuilder(),
            verifier = verifier)
}
