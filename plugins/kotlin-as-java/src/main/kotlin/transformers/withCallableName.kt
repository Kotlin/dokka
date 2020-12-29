package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.links.DRI

internal fun DRI.withCallableName(newName: String): DRI = copy(callable = callable?.copy(name = newName))