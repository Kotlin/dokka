package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.kotlinAsJava.converters.asJava
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DModuleView
import org.jetbrains.dokka.model.DPass
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

class KotlinAsJavaDocumentableTransformer : DocumentableTransformer {
    override fun invoke(original: DModuleView, context: DokkaContext): DModuleView = when (original) {
        is DPass -> original.copy(packages = original.packages.map { it.asJava() })
        is DModule -> original.copy(packages = original.packages.map { it.asJava() })
    }
}
