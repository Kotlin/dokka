package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.sourceSet
import org.jetbrains.dokka.utilities.cast

class SuppressedAnnotationsDocumentaryFilterTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context) {
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        if (d !is WithExtraProperties<*>) return false
        return d.cast<WithExtraProperties<out Documentable>>().hasOneOfAnnotations(sourceSet(d).suppressedAnnotations)
    }
}