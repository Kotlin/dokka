package org.jetbrains.dokka.javadoc

import com.sun.tools.doclets.formats.html.HtmlDoclet
import org.jetbrains.dokka.DocumentationOptions
import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.buildDocumentationModule
import java.io.File

/**
 * Test me, my friend
 */
public fun main(args: Array<String>) {
    val generator = DokkaGenerator(DokkaConsoleLogger, System.getProperty("java.class.path").split(File.pathSeparator), listOf(File("test").absolutePath), emptyList(), emptyList(), "me", "out/dokka", "html", emptyList(), false)
    val env = generator.createAnalysisEnvironment()
    val module = buildDocumentationModule(env, generator.moduleName, DocumentationOptions(includeNonPublic = true, sourceLinks = emptyList()), emptyList(), {
        generator.isSample(it)
    }, generator.logger)

    DokkaConsoleLogger.report()
    HtmlDoclet.start(ModuleNodeAdapter(module, StandardReporter))
}

public fun String.a(): Int  = 1