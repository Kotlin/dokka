/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal fun <T : Documentable> WithExtraProperties<T>.jvmField(): Annotations.Annotation? =
    extra[Annotations]?.directAnnotations?.entries?.firstNotNullOfOrNull { (_, annotations) -> annotations.jvmFieldAnnotation() }

internal fun List<Annotations.Annotation>.jvmFieldAnnotation(): Annotations.Annotation? =
    firstOrNull { it.dri.packageName == "kotlin.jvm" && it.dri.classNames == "JvmField" }

