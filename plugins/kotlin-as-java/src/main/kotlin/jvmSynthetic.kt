package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal fun <T : org.jetbrains.dokka.model.AnnotationTarget> WithExtraProperties<T>.hasJvmSynthetic(): Boolean {
    return extra[Annotations]
        ?.directAnnotations
        ?.entries
        ?.any { (_, annotations) ->
            annotations.any { it.dri.packageName == "kotlin.jvm" && it.dri.classNames == "JvmSynthetic" }
        } == true
}
