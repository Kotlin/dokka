package org.jetbrains.dokka.Formats

import org.jetbrains.dokka.*

abstract class KotlinFormatDescriptorBase : FormatDescriptor {
    override val packageDocumentationBuilderClass = KotlinPackageDocumentationBuilder::class
    override val javaDocumentationBuilderClass = KotlinJavaDocumentationBuilder::class

    override val generatorServiceClass = FileGenerator::class
}

class HtmlFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = HtmlFormatService::class
    override val outlineServiceClass = HtmlFormatService::class
}

class HtmlAsJavaFormatDescriptor : FormatDescriptor {
    override val formatServiceClass = HtmlFormatService::class
    override val outlineServiceClass = HtmlFormatService::class
    override val generatorServiceClass = FileGenerator::class
    override val packageDocumentationBuilderClass = KotlinAsJavaDocumentationBuilder::class
    override val javaDocumentationBuilderClass = JavaPsiDocumentationBuilder::class
}

class KotlinWebsiteFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = KotlinWebsiteFormatService::class
    override val outlineServiceClass = YamlOutlineService::class
}

class JekyllFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = JekyllFormatService::class
    override val outlineServiceClass = null
}

class MarkdownFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = MarkdownFormatService::class
    override val outlineServiceClass = null
}
