/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.internal

import java.net.URI

internal fun URI.appendPath(addition: String): URI {
    val currentPath = path.removeSuffix("/")
    val newPath = "$currentPath/$addition"
    return resolve(newPath).normalize()
}
