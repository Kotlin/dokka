package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.transformers.descriptors.DefaultDescriptorToDocumentationTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin

class DokkaBase: DokkaPlugin() {
    val defaultDescriptorToDocumentationTranslator by extending {
        CoreExtensions.descriptorToDocumentationTranslator providing ::DefaultDescriptorToDocumentationTranslator
    }
}