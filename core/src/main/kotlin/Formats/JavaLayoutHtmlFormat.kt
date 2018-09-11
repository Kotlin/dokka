package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import com.google.inject.Inject
import kotlinx.html.li
import kotlinx.html.stream.appendHTML
import kotlinx.html.ul
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Samples.DefaultSampleProcessingService
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toType
import java.io.File


class JavaLayoutHtmlFormatDescriptor : FormatDescriptor, DefaultAnalysisComponent {
    override val packageDocumentationBuilderClass = KotlinPackageDocumentationBuilder::class
    override val javaDocumentationBuilderClass = KotlinJavaDocumentationBuilder::class
    override val sampleProcessingService = DefaultSampleProcessingService::class
    override val elementSignatureProvider = KotlinElementSignatureProvider::class

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<Generator>() toType generatorServiceClass
    }

    val formatServiceClass = JavaLayoutHtmlFormatService::class
    val generatorServiceClass = JavaLayoutHtmlFormatGenerator::class
}


class JavaLayoutHtmlFormatService : FormatService {
    override val extension: String
        get() = TODO("not implemented")


    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder {
        TODO("not implemented")
    }
}

class JavaLayoutHtmlFormatOutputBuilder : FormattedOutputBuilder {
    override fun appendNodes(nodes: Iterable<DocumentationNode>) {

    }
}


class JavaLayoutHtmlFormatNavListBuilder @Inject constructor() : OutlineFormatService {
    override fun getOutlineFileName(location: Location): File {
        TODO()
    }

    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        with(to.appendHTML()) {
            //a(href = )
            li {
                when {
                    node.kind == NodeKind.Package -> appendOutline(location, to, node.members)
                }
            }
        }
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        with(to.appendHTML()) {
            ul { body() }
        }
    }

}

class JavaLayoutHtmlFormatGenerator @Inject constructor(
        private val outlineFormatService: OutlineFormatService
) : Generator {
    override fun buildPages(nodes: Iterable<DocumentationNode>) {

    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            if (node.kind == NodeKind.Module) {
                //outlineFormatService.formatOutline()
            }
        }
    }

    override fun buildSupportFiles() {}

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {

    }
}