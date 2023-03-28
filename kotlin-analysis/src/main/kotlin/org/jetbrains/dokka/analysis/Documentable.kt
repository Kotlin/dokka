package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement

class DescriptorDocumentableSource(val descriptor: DeclarationDescriptor) : DocumentableSource {
    override val path = descriptor.toSourceElement.containingFile.toString()
}
