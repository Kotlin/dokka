/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import java.net.URI
import java.net.URLEncoder

internal fun URI.appendPath(addition: String): URI {
    val currentPath = path.removeSuffix("/")
    val additionEncoded = addition
        .split("/")
        .joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8.name()).replace("+", "%20") }
    val newPath = "$currentPath/$additionEncoded"

    return URI(
        /*    scheme = */ scheme,
        /*  userInfo = */ userInfo,
        /*      host = */ host,
        /*      port = */ port,
        /*      path = */ newPath,
        /*     query = */ query,
        /*  fragment = */ fragment,
    ).normalize()
}
