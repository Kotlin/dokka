package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import kotlinx.html.li
import kotlinx.html.stream.appendHTML
import kotlinx.html.ul
import org.jetbrains.dokka.*
import java.io.File


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

    }
}


class JavaLayoutHtmlFormatNavListBuilder @Inject constructor(private val locationService: LocationService) : OutlineFormatService {
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