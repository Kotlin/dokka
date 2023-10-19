/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.utils

import java.io.File

fun File.copyInto(directory: File, overwrite: Boolean = false) =
    copyTo(directory.resolve(name), overwrite = overwrite)
