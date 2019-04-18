package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Samples.KotlinWebsiteSampleProcessingService
import org.jetbrains.dokka.Utilities.bind
import kotlin.reflect.KClass

abstract class KotlinFormatDescriptorBase
    : FileGeneratorBasedFormatDescriptor(),
        DefaultAnalysisComponent,
        DefaultAnalysisComponentServices by KotlinAsKotlin {
    override val generatorServiceClass = FileGenerator::class
    override val outlineServiceClass: KClass<out OutlineFormatService>? = null
    override val packageListServiceClass: KClass<out PackageListService>? = DefaultPackageListService::class
}

abstract class HtmlFormatDescriptorBase : FileGeneratorBasedFormatDescriptor(), DefaultAnalysisComponent {
    override val formatServiceClass = HtmlFormatService::class
    override val outlineServiceClass = HtmlFormatService::class
    override val generatorServiceClass = FileGenerator::class
    override val packageListServiceClass = DefaultPackageListService::class

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        super.configureOutput(binder)
        bind<HtmlTemplateService>().toProvider { HtmlTemplateService.default("style.css") }
    }
}

class HtmlFormatDescriptor : HtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsKotlin

class HtmlAsJavaFormatDescriptor : HtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsJava

class KotlinWebsiteHtmlFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = KotlinWebsiteHtmlFormatService::class
    override val sampleProcessingService = KotlinWebsiteSampleProcessingService::class
    override val outlineServiceClass = YamlOutlineService::class

    override fun configureOutput(binder: Binder) = with(binder) {
        super.configureOutput(binder)
        bind<HtmlTemplateService>().toInstance(EmptyHtmlTemplateService)
    }
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
