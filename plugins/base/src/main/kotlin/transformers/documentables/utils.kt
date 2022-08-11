package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExceptionInSupertypes
import org.jetbrains.dokka.model.properties.WithExtraProperties

/**
 * @return true if [T] has [kotlin.Deprecated] or [java.lang.Deprecated]
 *         annotation for **any** source set
 */
fun <T> T.isDeprecated() where T : WithExtraProperties<out Documentable> =
    deprecatedAnnotation != null


val <T> T.deprecatedAnnotation where T : WithExtraProperties<out Documentable>
    get() = extra[Annotations]?.let { annotations ->
        annotations.directAnnotations.values.flatten().firstOrNull {
            it.isDeprecated()
        }
    }

/**
 * @return true for [kotlin.Deprecated] and [java.lang.Deprecated]
 */
fun Annotations.Annotation.isDeprecated(): Boolean {
    return (this.dri.packageName == "kotlin" && this.dri.classNames == "Deprecated") ||
            (this.dri.packageName == "java.lang" && this.dri.classNames == "Deprecated")
}

val <T : WithExtraProperties<out Documentable>> T.isException: Boolean
    get() = extra[ExceptionInSupertypes] != null
