package org.jetbrains.dokka.Utilities

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.name.Names
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import java.io.File
import kotlin.reflect.KClass

const val impliedPlatformsName = "impliedPlatforms"

class DokkaAnalysisModule(val environment: AnalysisEnvironment,
                          val options: DocumentationOptions,
                          val defaultPlatformsProvider: DefaultPlatformsProvider,
                          val nodeReferenceGraph: NodeReferenceGraph,
                          val logger: DokkaLogger) : Module {
    override fun configure(binder: Binder) {
        binder.bind<DokkaLogger>().toInstance(logger)

        val coreEnvironment = environment.createCoreEnvironment()
        binder.bind<KotlinCoreEnvironment>().toInstance(coreEnvironment)

        val (dokkaResolutionFacade, libraryResolutionFacade) = environment.createResolutionFacade(coreEnvironment)
        binder.bind<DokkaResolutionFacade>().toInstance(dokkaResolutionFacade)
        binder.bind<DokkaResolutionFacade>().annotatedWith(Names.named("libraryResolutionFacade")).toInstance(libraryResolutionFacade)

        binder.bind<DocumentationOptions>().toInstance(options)

        binder.bind<DefaultPlatformsProvider>().toInstance(defaultPlatformsProvider)

        binder.bind<NodeReferenceGraph>().toInstance(nodeReferenceGraph)

        val descriptor = ServiceLocator.lookup<FormatDescriptor>("format", options.outputFormat)
        descriptor.configureAnalysis(binder)
    }
}

object StringListType : TypeLiteral<@JvmSuppressWildcards List<String>>()

class DokkaOutputModule(val options: DocumentationOptions,
                        val logger: DokkaLogger) : Module {
    override fun configure(binder: Binder) {
        binder.bind(File::class.java).annotatedWith(Names.named("outputDir")).toInstance(File(options.outputDir))

        binder.bind<DocumentationOptions>().toInstance(options)
        binder.bind<DokkaLogger>().toInstance(logger)
        binder.bind(StringListType).annotatedWith(Names.named(impliedPlatformsName)).toInstance(options.impliedPlatforms)
        binder.bind<String>().annotatedWith(Names.named("outlineRoot")).toInstance(options.outlineRoot)
        binder.bind<String>().annotatedWith(Names.named("dacRoot")).toInstance(options.dacRoot)
        binder.bind<Boolean>().annotatedWith(Names.named("generateClassIndex")).toInstance(options.generateClassIndexPage)
        binder.bind<Boolean>().annotatedWith(Names.named("generatePackageIndex")).toInstance(options.generatePackageIndexPage)
        val descriptor = ServiceLocator.lookup<FormatDescriptor>("format", options.outputFormat)

        descriptor.configureOutput(binder)
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


inline fun <reified T: Any> Binder.bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)

inline fun <reified T: Any> Binder.lazyBind(): Lazy<AnnotatedBindingBuilder<T>> = lazy { bind(T::class.java) }

inline infix fun <reified T: Any, TKClass: KClass<out T>> Lazy<AnnotatedBindingBuilder<T>>.toOptional(kClass: TKClass?) =
        kClass?.let { value toType it }

inline infix fun <reified T: Any, TKClass: KClass<out T>> AnnotatedBindingBuilder<T>.toType(kClass: TKClass) = to(kClass.java)
