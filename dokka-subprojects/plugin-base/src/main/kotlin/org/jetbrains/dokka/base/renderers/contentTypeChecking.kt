/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.renderers.HtmlFileExtensions.imageExtensions
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import java.io.File

public fun ContentEmbeddedResource.isImage(): Boolean {
    return File(address).extension.toLowerCase() in imageExtensions
}

public val String.URIExtension: String
    get() = substringBefore('?').substringAfterLast('.')

public fun String.isImage(): Boolean =
    URIExtension in imageExtensions

public object HtmlFileExtensions {
    public val imageExtensions: Set<String> = setOf("png", "jpg", "jpeg", "gif", "bmp", "tif", "webp", "svg")
}

