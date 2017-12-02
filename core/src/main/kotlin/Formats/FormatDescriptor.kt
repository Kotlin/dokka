package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.SampleProcessingService
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toOptional
import org.jetbrains.dokka.Utilities.toType
import kotlin.reflect.KClass


interface FormatDescriptorAnalysisComponent {
    fun configureAnalysis(binder: Binder)
}

interface FormatDescriptorOutputComponent {
    fun configureOutput(binder: Binder)
}

interface FormatDescriptor: FormatDescriptorAnalysisComponent, FormatDescriptorOutputComponent


abstract class FileGeneratorBasedFormatDescriptor : FormatDescriptor {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<OutlineFormatService>() toOptional (outlineServiceClass)
        bind<FormatService>() toOptional formatServiceClass
        bind<FileGenerator>() toType generatorServiceClass
        //bind<Generator>() toType  generatorServiceClass
        bind<PackageListService>() toOptional packageListServiceClass
    }

    abstract val formatServiceClass: KClass<out FormatService>?
    abstract val outlineServiceClass: KClass<out OutlineFormatService>?
    abstract val generatorServiceClass: KClass<out FileGenerator>
    abstract val packageListServiceClass: KClass<out PackageListService>?
}