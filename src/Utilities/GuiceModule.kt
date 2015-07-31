package org.jetbrains.dokka.Utilities

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Provider
import com.google.inject.name.Names
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import java.io.File

class GuiceModule(val config: DokkaGenerator) : Module {
    override fun configure(binder: Binder) {
        binder.bind(javaClass<DokkaGenerator>()).toInstance(config)
        binder.bind(javaClass<File>()).annotatedWith(Names.named("outputDir")).toInstance(File(config.outputDir))

        binder.bindNameAnnotated<LocationService, SingleFolderLocationService>("singleFolder")
        binder.bindNameAnnotated<FileLocationService, SingleFolderLocationService>("singleFolder")
        binder.bindNameAnnotated<LocationService, FoldersLocationService>("folders")
        binder.bindNameAnnotated<FileLocationService, FoldersLocationService>("folders")

        // defaults
        binder.bind(javaClass<LocationService>()).to(javaClass<FoldersLocationService>())
        binder.bind(javaClass<FileLocationService>()).to(javaClass<FoldersLocationService>())
        binder.bind(javaClass<LanguageService>()).to(javaClass<KotlinLanguageService>())

        binder.bind(javaClass<HtmlTemplateService>()).toProvider(object : Provider<HtmlTemplateService> {
            override fun get(): HtmlTemplateService = HtmlTemplateService.default("/dokka/styles/style.css")
        })

        binder.registerCategory<LanguageService>("language")
        binder.registerCategory<OutlineFormatService>("outline")
        binder.registerCategory<FormatService>("format")
        binder.registerCategory<Generator>("generator")

        val descriptor = ServiceLocator.lookup<FormatDescriptor>("format", config.outputFormat, config)

        descriptor.outlineServiceClass?.let { clazz ->
            binder.bind(javaClass<OutlineFormatService>()).to(clazz)
        }
        descriptor.formatServiceClass?.let { clazz ->
            binder.bind(javaClass<FormatService>()).to(clazz)
        }
        binder.bind(javaClass<Generator>()).to(descriptor.generatorServiceClass)
    }

}

private inline fun <reified T: Any> Binder.registerCategory(category: String) {
    ServiceLocator.allServices(category).forEach {
        @Suppress("UNCHECKED_CAST")
        bind(javaClass<T>()).annotatedWith(Names.named(it.name)).to(javaClass<T>().classLoader.loadClass(it.className) as Class<T>)
    }
}

private inline fun <reified Base : Any, reified T : Base> Binder.bindNameAnnotated(name: String) {
    bind(javaClass<Base>()).annotatedWith(Names.named(name)).to(javaClass<T>())
}

