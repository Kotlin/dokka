/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.links.DRI

internal fun DRI.withCallableName(newName: String): DRI = copy(callable = callable?.copy(name = newName))
