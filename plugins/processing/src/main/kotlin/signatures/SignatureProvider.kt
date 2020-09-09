package org.jetbrains.dokka.processing.signatures

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.ContentNode

interface SignatureProvider {
    fun signature(documentable: Documentable): List<ContentNode>
}
