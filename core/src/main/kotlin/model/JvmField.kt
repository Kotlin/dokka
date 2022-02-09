package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

fun DRI.isJvmField(): Boolean = packageName == "kotlin.jvm" && classNames == "JvmField"

fun Annotations.Annotation.isJvmField(): Boolean = dri.isJvmName()
