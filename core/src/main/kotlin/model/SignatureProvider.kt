package org.jetbrains.dokka.model

import org.jetbrains.dokka.pages.ContentNode

interface SignatureProvider {
    fun signature(documentable: Documentable): List<ContentNode>
}