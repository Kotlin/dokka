package org.jetbrains.dokka.javadoc

import Formats.AsciidocFormatService
import Formats.CodeRayStyleSheet
import Formats.selectStylesheet
import com.google.inject.Inject
import com.sun.tools.doclets.formats.html.HtmlDoclet
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor
import org.jetbrains.dokka.Samples.DefaultSampleProcessingService
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.KClass


/**
 * @author Mario Toffia
 */
class JavaAsciidocGenerator @Inject constructor(val options: DocumentationOptions, val logger: DokkaLogger) :
    Generator {

    @set:Inject(optional = true) lateinit var formatService: FormatService

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single() as DocumentationModule
        val fs = formatService as StructuredFormatService

        DokkaConsoleLogger.report()
        HtmlDoclet.start(ModuleNodeAdapter(module, StandardReporter(logger), options.outputDir, fs))
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        // no outline could be generated separately
    }

    override fun buildSupportFiles() {
        FileOutputStream(File(options.outputDir, "stylesheet.css"), false).use {
            javaClass.getResourceAsStream(selectStylesheet()).copyTo(it)
        }

        FileOutputStream(File(options.outputDir, "coderay-asciidoctor.css"), false).use {
            javaClass.getResourceAsStream(CodeRayStyleSheet).copyTo(it)
        }
    }

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {
        // handled by javadoc itself
    }
}

class JavaAsciidocFormatDescriptor : FormatDescriptor {
    override val formatServiceClass = AsciidocFormatService::class
    override val outlineServiceClass = AsciidocFormatService::class
    override val generatorServiceClass = JavaAsciidocGenerator::class
    override val packageDocumentationBuilderClass = KotlinAsJavaDocumentationBuilder::class
    override val javaDocumentationBuilderClass = JavaPsiDocumentationBuilder::class
    override val sampleProcessingService = DefaultSampleProcessingService::class
    override val packageListServiceClass: KClass<out PackageListService>? = null
}