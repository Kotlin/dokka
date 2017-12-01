package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.SampleProcessingService
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toOptional
import org.jetbrains.dokka.Utilities.toType
import kotlin.reflect.KClass

interface FormatDescriptor {
    fun configureAnalysis(binder: Binder)
    fun configureOutput(binder: Binder)
}

interface FormatDescriptorAnalysisComponentProvider : FormatDescriptor {

    val packageDocumentationBuilderClass: KClass<out PackageDocumentationBuilder>
    val javaDocumentationBuilderClass: KClass<out JavaDocumentationBuilder>
    val sampleProcessingService: KClass<out SampleProcessingService>
    val descriptorSignatureProvider: KClass<out DescriptorSignatureProvider>


    override fun configureAnalysis(binder: Binder): Unit = with(binder) {
        bind<DescriptorSignatureProvider>() toType descriptorSignatureProvider
        bind<PackageDocumentationBuilder>() toType packageDocumentationBuilderClass
        bind<JavaDocumentationBuilder>() toType javaDocumentationBuilderClass
        bind<SampleProcessingService>() toType sampleProcessingService
    }
}

abstract class FileGeneratorBasedFormatDescriptor : FormatDescriptor, FormatDescriptorAnalysisComponentProvider {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<OutlineFormatService>() toOptional (outlineServiceClass)
        bind<FormatService>() toOptional formatServiceClass
        bind<FileGenerator>() toType  generatorServiceClass
        //bind<Generator>() toType  generatorServiceClass
        bind<PackageListService>() toOptional packageListServiceClass
    }

    abstract val formatServiceClass: KClass<out FormatService>?
    abstract val outlineServiceClass: KClass<out OutlineFormatService>?
    abstract val generatorServiceClass: KClass<out FileGenerator>
    abstract val packageListServiceClass: KClass<out PackageListService>?
}