package org.jetbrains.dokka

import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.transformers.PageNodeTransformer

object CoreExtensions {
    val pageTransformer = ExtensionPoint<PageNodeTransformer>(this::class.qualifiedName!!, "pageTransformer")
}