package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.isJvmName
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal fun WithExtraProperties<out Documentable>.directlyAnnotatedJvmName(): Annotations.Annotation? =
    extra[Annotations]?.directAnnotations?.entries?.firstNotNullResult { (_, annotations) -> annotations.jvmNameAnnotation() }

internal fun WithExtraProperties<out Documentable>.fileLevelJvmName(): Annotations.Annotation? =
    extra[Annotations]?.fileLevelAnnotations?.entries?.firstNotNullResult { (_, annotations) -> annotations.jvmNameAnnotation() }

internal fun List<Annotations.Annotation>.jvmNameAnnotation(): Annotations.Annotation? =
    firstOrNull { it.isJvmName() }

internal fun Annotations.Annotation.jvmNameAsString(): String? = (params["name"] as? StringValue)?.value