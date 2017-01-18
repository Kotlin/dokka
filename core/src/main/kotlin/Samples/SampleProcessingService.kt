package org.jetbrains.dokka.Samples

import org.jetbrains.dokka.ContentNode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

interface SampleProcessingService {
    fun resolveSample(descriptor: DeclarationDescriptor, functionName: String?, kdocTag: KDocTag): ContentNode
}