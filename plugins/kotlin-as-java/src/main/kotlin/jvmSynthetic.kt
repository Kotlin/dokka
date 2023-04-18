package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal fun WithExtraProperties<out Documentable>.hasJvmSynthetic(): Boolean {
    @Suppress("TYPE_MISMATCH_WARNING_FOR_INCORRECT_CAPTURE_APPROXIMATION")
    return extra[Annotations]
        ?.directAnnotations
        ?.entries
        ?.any { (_, annotations) ->
            annotations.any { it.dri.packageName == "kotlin.jvm" && it.dri.classNames == "JvmSynthetic" }
        } == true
}
