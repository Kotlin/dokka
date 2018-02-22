package org.jetbrains.dokka.Utilities

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Provider
import com.google.inject.TypeLiteral
import com.google.inject.name.Names
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.SampleProcessingService
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import java.io.File

const val impliedPlatformsName = "impliedPlatforms"

class DokkaAnalysisModule(val environment: AnalysisEnvironment,
                          val options: DocumentationOptions,
                          val defaultPlatformsProvider: DefaultPlatformsProvider,
                          val nodeReferenceGraph: NodeReferenceGraph,
                          val logger: DokkaLogger) : Module {
    override fun configure(binder: Binder) {
        binder.bind<DokkaLogger>().toInstance(logger)

        val descriptor = ServiceLocator.lookup<FormatDescriptor>("format", options.outputFormat)
        binder.bind<DescriptorSignatureProvider>().to(descriptor.descriptorSignatureProvider.java)
        binder.registerCategory<LanguageService>("language")
        binder.bind<PackageDocumentationBuilder>().to(descriptor.packageDocumentationBuilderClass.java)
        binder.bind<JavaDocumentationBuilder>().to(descriptor.javaDocumentationBuilderClass.java)
        binder.bind<SampleProcessingService>().to(descriptor.sampleProcessingService.java)

        val coreEnvironment = environment.createCoreEnvironment()
        binder.bind<KotlinCoreEnvironment>().toInstance(coreEnvironment)

        val dokkaResolutionFacade = environment.createResolutionFacade(coreEnvironment)
        binder.bind<DokkaResolutionFacade>().toInstance(dokkaResolutionFacade)

        binder.bind<DocumentationOptions>().toInstance(options)

        binder.bind<DefaultPlatformsProvider>().toInstance(defaultPlatformsProvider)

        binder.bind<NodeReferenceGraph>().toInstance(nodeReferenceGraph)
    }
}

object StringListType : TypeLiteral<@JvmSuppressWildcards List<String>>()

class DokkaOutputModule(val options: DocumentationOptions,
                        val logger: DokkaLogger) : Module {
    override fun configure(binder: Binder) {
        binder.bind(LanguageService::class.java).to(KotlinLanguageService::class.java)

        binder.bind(HtmlTemplateService::class.java).toProvider(object : Provider<HtmlTemplateService> {
            override fun get(): HtmlTemplateService = HtmlTemplateService.default("style.css")
        })

        binder.bind(File::class.java).annotatedWith(Names.named("outputDir")).toInstance(File(options.outputDir))

        binder.bindNameAnnotated<LocationService, SingleFolderLocationService>("singleFolder")
        binder.bindNameAnnotated<FileLocationService, SingleFolderLocationService>("singleFolder")
        binder.bindNameAnnotated<LocationService, FoldersLocationService>("folders")
        binder.bindNameAnnotated<FileLocationService, FoldersLocationService>("folders")

        // defaults
        binder.bind(LocationService::class.java).to(FoldersLocationService::class.java)
        binder.bind(FileLocationService::class.java).to(FoldersLocationService::class.java)

        binder.registerCategory<OutlineFormatService>("outline")
        binder.registerCategory<FormatService>("format")
        binder.registerCategory<Generator>("generator")

        val descriptor = ServiceLocator.lookup<FormatDescriptor>("format", options.outputFormat)

        descriptor.outlineServiceClass?.let { clazz ->
            binder.bind(OutlineFormatService::class.java).to(clazz.java)
        }
        descriptor.formatServiceClass?.let { clazz ->
            binder.bind(FormatService::class.java).to(clazz.java)
        }

        binder.bind<Generator>().to(descriptor.generatorServiceClass.java)

        descriptor.packageListServiceClass?.let { binder.bind<PackageListService>().to(it.java) }

        binder.bind<DocumentationOptions>().toInstance(options)
        binder.bind<DokkaLogger>().toInstance(logger)
        binder.bind(StringListType).annotatedWith(Names.named(impliedPlatformsName)).toInstance(options.impliedPlatforms)
    }
}

private inline fun <reified T: Any> Binder.registerCategory(category: String) {
    ServiceLocator.allServices(category).forEach {
        @Suppress("UNCHECKED_CAST")
        bind(T::class.java).annotatedWith(Names.named(it.name)).to(T::class.java.classLoader.loadClass(it.className) as Class<T>)
    }
}

private inline fun <reified Base : Any, reified T : Base> Binder.bindNameAnnotated(name: String) {
    bind(Base::class.java).annotatedWith(Names.named(name)).to(T::class.java)
}


inline fun <reified T: Any> Binder.bind() = bind(T::class.java)
