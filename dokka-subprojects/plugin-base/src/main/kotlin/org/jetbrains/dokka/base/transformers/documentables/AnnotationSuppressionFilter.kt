/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

public class AnnotationSuppressionFilter(
    public val dokkaContext: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {

    private val suppressedAnnotations by lazy {
        dokkaContext.configuration.sourceSets.flatMap { it.suppressedAnnotations }.toSet()
    }

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        if (suppressedAnnotations.isEmpty()) return false

        @Suppress("UNCHECKED_CAST")
        val annotations =
            (d as? WithExtraProperties<AnnotationTarget>)?.extra?.get(Annotations as org.jetbrains.dokka.model.properties.ExtraProperty.Key<AnnotationTarget, Annotations>)
        return annotations?.directAnnotations?.values?.flatten()?.any { annotation ->
            val fqName = listOfNotNull(annotation.dri.packageName, annotation.dri.classNames).joinToString(".")
            fqName in suppressedAnnotations
        } ?: false
    }
}
