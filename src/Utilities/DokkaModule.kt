package org.jetbrains.dokka.Utilities

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Provider
import com.google.inject.name.Names
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import java.io.File

class DokkaModule(val environment: AnalysisEnvironment,
                  val options: DocumentationOptions,
                  val logger: DokkaLogger) : Module {
    override fun configure(binder: Binder) {
        binder.bind(File::class.java).annotatedWith(Names.named("outputDir")).toInstance(File(options.outputDir))

        binder.bindNameAnnotated<LocationService, SingleFolderLocationService>("singleFolder")
        binder.bindNameAnnotated<FileLocationService, SingleFolderLocationService>("singleFolder")
        binder.bindNameAnnotated<LocationService, FoldersLocationService>("folders")
        binder.bindNameAnnotated<FileLocationService, FoldersLocationService>("folders")

        // defaults
        binder.bind(LocationService::class.java).to(FoldersLocationService::class.java)
        binder.bind(FileLocationService::class.java).to(FoldersLocationService::class.java)
        binder.bind(LanguageService::class.java).to(KotlinLanguageService::class.java)

        binder.bind(HtmlTemplateService::class.java).toProvider(object : Provider<HtmlTemplateService> {
            override fun get(): HtmlTemplateService = HtmlTemplateService.default("/dokka/styles/style.css")
        })

        binder.registerCategory<LanguageService>("language")
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
        binder.bind<PackageDocumentationBuilder>().to(descriptor.packageDocumentationBuilderClass.java)
        binder.bind<JavaDocumentationBuilder>().to(descriptor.javaDocumentationBuilderClass.java)

        binder.bind<Generator>().to(descriptor.generatorServiceClass.java)

        val coreEnvironment = environment.createCoreEnvironment()
        binder.bind<KotlinCoreEnvironment>().toInstance(coreEnvironment)

        val dokkaResolutionFacade = environment.createResolutionFacade(coreEnvironment)
        binder.bind<DokkaResolutionFacade>().toInstance(dokkaResolutionFacade)

        binder.bind<DocumentationOptions>().toInstance(options)
        binder.bind<DokkaLogger>().toInstance(logger)
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
