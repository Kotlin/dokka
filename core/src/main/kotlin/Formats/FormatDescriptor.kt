package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.*
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.SampleProcessingService
import kotlin.reflect.KClass

interface FormatDescriptor {
    val formatServiceClass: KClass<out FormatService>?
    val outlineServiceClass: KClass<out OutlineFormatService>?
    val generatorServiceClass: KClass<out Generator>
    val packageDocumentationBuilderClass: KClass<out PackageDocumentationBuilder>
    val javaDocumentationBuilderClass: KClass<out JavaDocumentationBuilder>
    val sampleProcessingService: KClass<out SampleProcessingService>
    val packageListServiceClass: KClass<out PackageListService>?
    val descriptorSignatureProvider: KClass<out DescriptorSignatureProvider>
}
