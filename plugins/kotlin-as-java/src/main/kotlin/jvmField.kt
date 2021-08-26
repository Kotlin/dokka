package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal fun WithExtraProperties<out Documentable>.jvmField(): Annotations.Annotation? =
    extra[Annotations]?.directAnnotations?.entries?.firstNotNullOfOrNull { (_, annotations) -> annotations.jvmFieldAnnotation() }

internal fun List<Annotations.Annotation>.jvmFieldAnnotation(): Annotations.Annotation? =
    firstOrNull { it.dri.packageName == "kotlin.jvm" && it.dri.classNames == "JvmField" }