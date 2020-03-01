package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.kotlinAsJava.converters.asJava
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

class KotlinAsJavaDocumentableTransformer : DocumentableTransformer {
    override fun invoke(original: Module, context: DokkaContext): Module =
        original.copy(packages = original.packages.map { it.asJava() })
}
