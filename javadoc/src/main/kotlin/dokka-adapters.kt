package org.jetbrains.dokka.javadoc

import com.google.inject.Inject
import com.sun.tools.doclets.formats.html.HtmlDoclet
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.FormatDescriptor

class JavadocGenerator @Inject constructor (val conf: DokkaGenerator) : Generator {
    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single() as DocumentationModule

        DokkaConsoleLogger.report()
        HtmlDoclet.start(ModuleNodeAdapter(module, StandardReporter(conf.logger), conf.outputDir))
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        // no outline could be generated separately
    }
}

class JavadocFormatDescriptor : FormatDescriptor {
    override val formatServiceClass: Class<out FormatService>?
        get() = null
    override val outlineServiceClass: Class<out OutlineFormatService>?
        get() = null

    override val generatorServiceClass: Class<out Generator>
        get() = JavadocGenerator::class.java

    override val packageDocumentationBuilderServiceClass: Class<out PackageDocumentationBuilder>?
        get() = KotlinAsJavaDocumentationBuilder::class.java
}
