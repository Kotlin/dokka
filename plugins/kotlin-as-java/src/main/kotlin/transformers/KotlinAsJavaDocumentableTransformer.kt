package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.kotlinAsJava.converters.asJava
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.base.plugability.DokkaContext
import org.jetbrains.dokka.base.transformers.documentables.DocumentableTransformer

class KotlinAsJavaDocumentableTransformer :
    DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule =
        original.copy(packages = original.packages.map { it.asJava() })
}
