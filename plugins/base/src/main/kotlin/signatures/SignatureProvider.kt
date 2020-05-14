package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentNode

interface SignatureProvider {
    fun signature(documentable: Documentable): ContentNode
}