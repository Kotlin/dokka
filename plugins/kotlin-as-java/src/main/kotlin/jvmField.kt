package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.util.firstNotNullResult

internal fun <T : Documentable> WithExtraProperties<T>.jvmField(): Annotations.Annotation? =
    extra[Annotations]?.directAnnotations?.entries?.firstNotNullResult { (_, annotations) -> annotations.jvmFieldAnnotation() }

internal fun List<Annotations.Annotation>.jvmFieldAnnotation(): Annotations.Annotation? =
    firstOrNull { it.dri.packageName == "kotlin.jvm" && it.dri.classNames == "JvmField" }
