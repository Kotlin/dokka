package org.jetbrains.dokka.javadoc

import com.google.inject.Inject
import com.sun.tools.doclets.formats.html.HtmlDoclet
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import org.jetbrains.dokka.Kotlin.KotlinAsJavaDescriptorSignatureProvider
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.dokka.Samples.DefaultSampleProcessingService
import kotlin.reflect.KClass

class JavadocGenerator @Inject constructor(val options: DocumentationOptions, val logger: DokkaLogger) : Generator {

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single() as DocumentationModule

        DokkaConsoleLogger.report()
        HtmlDoclet.start(ModuleNodeAdapter(module, StandardReporter(logger), options.outputDir))
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        // no outline could be generated separately
    }

    override fun buildSupportFiles() {
    }

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {
        // handled by javadoc itself
    }
}

class JavadocFormatDescriptor : FormatDescriptor {
    override val formatServiceClass = null
    override val outlineServiceClass = null
    override val generatorServiceClass = JavadocGenerator::class
    override val packageDocumentationBuilderClass = KotlinAsJavaDocumentationBuilder::class
    override val javaDocumentationBuilderClass = JavaPsiDocumentationBuilder::class
    override val sampleProcessingService = DefaultSampleProcessingService::class
    override val packageListServiceClass: KClass<out PackageListService>? = null
    override val descriptorSignatureProvider = KotlinAsJavaDescriptorSignatureProvider::class
}
