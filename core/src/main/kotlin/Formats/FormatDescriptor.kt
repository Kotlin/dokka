package org.jetbrains.dokka.Formats

import Samples.SampleProcessingService
import org.jetbrains.dokka.*
import kotlin.reflect.KClass

interface FormatDescriptor {
    val formatServiceClass: KClass<out FormatService>?
    val outlineServiceClass: KClass<out OutlineFormatService>?
    val generatorServiceClass: KClass<out Generator>
    val packageDocumentationBuilderClass: KClass<out PackageDocumentationBuilder>
    val javaDocumentationBuilderClass: KClass<out JavaDocumentationBuilder>
    val sampleProcessingService: KClass<out SampleProcessingService>
}
