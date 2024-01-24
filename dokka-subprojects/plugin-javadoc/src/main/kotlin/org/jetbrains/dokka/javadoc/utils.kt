/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import java.nio.file.Path
import java.nio.file.Paths

internal fun Path.toNormalized() = this.normalize().toFile().toString()

internal fun String.toNormalized() = Paths.get(this).toNormalized()
