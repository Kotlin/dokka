/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.model.isJvmName
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal fun <T : Documentable> WithExtraProperties<T>.directlyAnnotatedJvmName(): Annotations.Annotation? =
    extra[Annotations]?.directAnnotations?.entries?.firstNotNullOfOrNull { (_, annotations)-> annotations.jvmNameAnnotation() }

internal fun <T : Documentable> WithExtraProperties<T>.fileLevelJvmName(): Annotations.Annotation? =
    extra[Annotations]?.fileLevelAnnotations?.entries?.firstNotNullOfOrNull { (_, annotations) -> annotations.jvmNameAnnotation() }

internal fun List<Annotations.Annotation>.jvmNameAnnotation(): Annotations.Annotation? =
    firstOrNull { it.isJvmName() }

internal fun Annotations.Annotation.jvmNameAsString(): String? = (params["name"] as? StringValue)?.value

