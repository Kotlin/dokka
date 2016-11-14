package org.jetbrains.dokka.Samples

import org.jetbrains.dokka.ContentNode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

interface SampleProcessingService {
    fun resolveSample(descriptor: DeclarationDescriptor, functionName: String?): ContentNode
}