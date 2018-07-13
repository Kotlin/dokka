package org.jetbrains.dokka.javadoc

import com.google.inject.Binder
import com.google.inject.Inject
import com.sun.tools.doclets.formats.html.HtmlDoclet
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.*
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.toType

class JavadocGenerator @Inject constructor(val options: DocumentationOptions, val logger: DokkaLogger) : Generator {

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single() as DocumentationModule

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

class JavadocFormatDescriptor :
        FormatDescriptor,
        DefaultAnalysisComponent,
        DefaultAnalysisComponentServices by KotlinAsJava {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<Generator>() toType JavadocGenerator::class
    }
}
