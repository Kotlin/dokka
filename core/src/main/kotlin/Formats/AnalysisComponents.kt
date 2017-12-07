package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Kotlin.KotlinAsJavaDescriptorSignatureProvider
import org.jetbrains.dokka.Kotlin.KotlinDescriptorSignatureProvider
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.DefaultSampleProcessingService
import org.jetbrains.dokka.Samples.SampleProcessingService
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toType
import kotlin.reflect.KClass


interface DefaultAnalysisComponentServices {
    val packageDocumentationBuilderClass: KClass<out PackageDocumentationBuilder>
    val javaDocumentationBuilderClass: KClass<out JavaDocumentationBuilder>
    val sampleProcessingService: KClass<out SampleProcessingService>
    val descriptorSignatureProvider: KClass<out DescriptorSignatureProvider>
}

interface DefaultAnalysisComponent : FormatDescriptorAnalysisComponent, DefaultAnalysisComponentServices {
    override fun configureAnalysis(binder: Binder): Unit = with(binder) {
        bind<DescriptorSignatureProvider>() toType descriptorSignatureProvider
        bind<PackageDocumentationBuilder>() toType packageDocumentationBuilderClass
        bind<JavaDocumentationBuilder>() toType javaDocumentationBuilderClass
        bind<SampleProcessingService>() toType sampleProcessingService
    }
}


object KotlinAsJava : DefaultAnalysisComponentServices {
    override val packageDocumentationBuilderClass = KotlinAsJavaDocumentationBuilder::class
    override val javaDocumentationBuilderClass = JavaPsiDocumentationBuilder::class
    override val sampleProcessingService = DefaultSampleProcessingService::class
    override val descriptorSignatureProvider = KotlinAsJavaDescriptorSignatureProvider::class
}


object KotlinAsKotlin : DefaultAnalysisComponentServices {
    override val packageDocumentationBuilderClass = KotlinPackageDocumentationBuilder::class
    override val javaDocumentationBuilderClass = KotlinJavaDocumentationBuilder::class
    override val sampleProcessingService = DefaultSampleProcessingService::class
    override val descriptorSignatureProvider = KotlinDescriptorSignatureProvider::class
}