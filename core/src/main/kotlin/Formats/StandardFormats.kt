package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.*
import org.jetbrains.dokka.Kotlin.KotlinAsJavaDescriptorSignatureProvider
import org.jetbrains.dokka.Kotlin.KotlinDescriptorSignatureProvider
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.DefaultSampleProcessingService
import org.jetbrains.dokka.Samples.KotlinWebsiteSampleProcessingService
import org.jetbrains.dokka.Samples.SampleProcessingService
import kotlin.reflect.KClass

abstract class KotlinFormatDescriptorBase : FormatDescriptor {
    override val packageDocumentationBuilderClass = KotlinPackageDocumentationBuilder::class
    override val javaDocumentationBuilderClass = KotlinJavaDocumentationBuilder::class

    override val generatorServiceClass = FileGenerator::class
    override val outlineServiceClass: KClass<out OutlineFormatService>? = null
    override val sampleProcessingService: KClass<out SampleProcessingService> = DefaultSampleProcessingService::class
    override val packageListServiceClass: KClass<out PackageListService>? = DefaultPackageListService::class
    override val descriptorSignatureProvider = KotlinDescriptorSignatureProvider::class
}

class HtmlFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = HtmlFormatService::class
    override val outlineServiceClass = HtmlFormatService::class
}

class HtmlAsJavaFormatDescriptor : FormatDescriptor {
    override val formatServiceClass = HtmlFormatService::class
    override val outlineServiceClass = HtmlFormatService::class
    override val generatorServiceClass = FileGenerator::class
    override val packageDocumentationBuilderClass = KotlinAsJavaDocumentationBuilder::class
    override val javaDocumentationBuilderClass = JavaPsiDocumentationBuilder::class
    override val sampleProcessingService: KClass<out SampleProcessingService> = DefaultSampleProcessingService::class
    override val packageListServiceClass: KClass<out PackageListService>? = DefaultPackageListService::class
    override val descriptorSignatureProvider = KotlinAsJavaDescriptorSignatureProvider::class
}

class KotlinWebsiteFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = KotlinWebsiteFormatService::class
    override val outlineServiceClass = YamlOutlineService::class
}

class KotlinWebsiteFormatRunnableSamplesDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = KotlinWebsiteRunnableSamplesFormatService::class
    override val sampleProcessingService = KotlinWebsiteSampleProcessingService::class
    override val outlineServiceClass = YamlOutlineService::class
}

class KotlinWebsiteHtmlFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = KotlinWebsiteHtmlFormatService::class
    override val sampleProcessingService = KotlinWebsiteSampleProcessingService::class
    override val outlineServiceClass = YamlOutlineService::class
}

class JekyllFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = JekyllFormatService::class
}

class MarkdownFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = MarkdownFormatService::class
}

class GFMFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = GFMFormatService::class
}
