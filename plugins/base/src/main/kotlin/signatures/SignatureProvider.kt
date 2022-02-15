package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.ContentNode

fun interface SignatureProvider {
    fun signature(documentable: Documentable): List<ContentNode>
}
