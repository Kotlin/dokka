package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.lazyBind
import org.jetbrains.dokka.Utilities.toOptional
import org.jetbrains.dokka.Utilities.toType
import kotlin.reflect.KClass


interface FormatDescriptorAnalysisComponent {
    fun configureAnalysis(binder: Binder)
}

interface FormatDescriptorOutputComponent {
    fun configureOutput(binder: Binder)
}

interface FormatDescriptor : FormatDescriptorAnalysisComponent, FormatDescriptorOutputComponent


abstract class FileGeneratorBasedFormatDescriptor : FormatDescriptor {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<Generator>() toType NodeLocationAwareGenerator::class
        bind<NodeLocationAwareGenerator>() toType generatorServiceClass
        bind(generatorServiceClass.java) // https://github.com/google/guice/issues/847

        bind<LanguageService>() toType languageServiceClass

        lazyBind<OutlineFormatService>() toOptional (outlineServiceClass)
        lazyBind<FormatService>() toOptional formatServiceClass
        lazyBind<PackageListService>() toOptional packageListServiceClass
    }

    abstract val formatServiceClass: KClass<out FormatService>?
    abstract val outlineServiceClass: KClass<out OutlineFormatService>?
    abstract val generatorServiceClass: KClass<out FileGenerator>
    abstract val packageListServiceClass: KClass<out PackageListService>?

    open val languageServiceClass: KClass<out LanguageService> = KotlinLanguageService::class
}