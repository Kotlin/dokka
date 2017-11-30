package org.jetbrains.dokka.formats

import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.KotlinFormatDescriptorBase


class JavaLayoutHtmlFormatDescriptor : KotlinFormatDescriptorBase() {
    override val formatServiceClass = JavaLayoutHtmlFormatService::class
    override val generatorServiceClass = JavaLayoutHtmlFormatGenerator::class
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
        TODO("not implemented")
    }
}

class JavaLayoutHtmlFormatGenerator : Generator {
    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        TODO("not implemented")
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        TODO("not implemented")
    }

    override fun buildSupportFiles() {
        TODO("not implemented")
    }

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {
        TODO("not implemented")
    }
}