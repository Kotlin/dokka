package org.example.dokka.plugin

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin

class HideInternalApiPlugin : DokkaPlugin() {
    val myFilterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalApiTransformer
    }
}

class HideInternalApiTransformer(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations: List<Annotations.Annotation> =
            (d as? WithExtraProperties<*>)
                ?.extra
                ?.allOfType<Annotations>()
                ?.flatMap { it.directAnnotations.values.flatten() }
                ?: emptyList()

        return annotations.any { isInternalAnnotation(it) }
    }

    private fun isInternalAnnotation(annotation: Annotations.Annotation): Boolean {
        return annotation.dri.packageName == "org.jetbrains.dokka.internal.test"
                && annotation.dri.classNames == "Internal"
    }
}
