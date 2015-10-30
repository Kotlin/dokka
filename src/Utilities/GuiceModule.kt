package org.jetbrains.dokka.Utilities

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Provider
import com.google.inject.name.Names
import com.google.inject.util.Providers
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import java.io.File

class GuiceModule(val config: DokkaGenerator) : Module {
    override fun configure(binder: Binder) {
        binder.bind(DokkaGenerator::class.java).toInstance(config)
        binder.bind(File::class.java).annotatedWith(Names.named("outputDir")).toInstance(File(config.outputDir))

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

        val descriptor = ServiceLocator.lookup<FormatDescriptor>("format", config.outputFormat, config)

        descriptor.outlineServiceClass?.let { clazz ->
            binder.bind(OutlineFormatService::class.java).to(clazz)
        }
        descriptor.formatServiceClass?.let { clazz ->
            binder.bind(FormatService::class.java).to(clazz)
        }
        if (descriptor.packageDocumentationBuilderServiceClass != null) {
            binder.bind(PackageDocumentationBuilder::class.java).to(descriptor.packageDocumentationBuilderServiceClass)
        } else {
            binder.bind(PackageDocumentationBuilder::class.java).toProvider(Providers.of(null))
        }

        binder.bind(Generator::class.java).to(descriptor.generatorServiceClass)
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

