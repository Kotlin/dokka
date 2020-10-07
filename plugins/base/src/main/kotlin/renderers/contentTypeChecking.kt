package org.jetbrains.dokka.base.renderers

import org.jetbrains.dokka.base.renderers.HtmlFileExtensions.imageExtensions
import org.jetbrains.dokka.pages.ContentEmbeddedResource
import java.io.File

fun ContentEmbeddedResource.isImage(): Boolean {
    return File(address).extension.toLowerCase() in imageExtensions
}

fun String.isImage(): Boolean =
    substringBefore('?').substringAfterLast('.') in imageExtensions

object HtmlFileExtensions {
    val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "tif", "webp", "svg")
}