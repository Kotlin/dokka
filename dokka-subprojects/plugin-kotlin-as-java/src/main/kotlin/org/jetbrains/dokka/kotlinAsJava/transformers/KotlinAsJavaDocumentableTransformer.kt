/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.kotlinAsJava.converters.KotlinToJavaConverter
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

public class KotlinAsJavaDocumentableTransformer : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule =
        original.copy(packages = original.packages.map {
            with(KotlinToJavaConverter(context)) {
                it.asJava()
            }
        })
}
