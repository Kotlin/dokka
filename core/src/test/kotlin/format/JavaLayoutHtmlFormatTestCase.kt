package org.jetbrains.dokka.tests

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.name.Names
import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.DocumentationOptions
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.Formats.JavaLayoutHtmlFormatDescriptorBase
import org.jetbrains.dokka.Formats.JavaLayoutHtmlFormatGenerator
import org.jetbrains.dokka.Generator
import org.jetbrains.dokka.Utilities.bind
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI

abstract class JavaLayoutHtmlFormatTestCase {

    abstract val formatDescriptor: JavaLayoutHtmlFormatDescriptorBase

    @get:Rule
    var folder = TemporaryFolder()

    var options =
        DocumentationOptions(
            "",
            "java-layout-html",
            apiVersion = null,
            languageVersion = null,
            generateClassIndexPage = false,
            generatePackageIndexPage = false,
            noStdlibLink = false,
            noJdkLink = false,
            collectInheritedExtensionsFromLibraries = true
        )

    val injector: Injector by lazy {
        Guice.createInjector(Module { binder ->
            binder.bind<File>().annotatedWith(Names.named("outputDir")).toInstance(folder.apply { create() }.root)

            binder.bind<DocumentationOptions>().toProvider { options }
            binder.bind<DokkaLogger>().toInstance(object : DokkaLogger {
                override fun info(message: String) {
                    println(message)
                }

                override fun warn(message: String) {
                    println("WARN: $message")
                }

                override fun error(message: String) {
                    println("ERROR: $message")
                }

            })

            formatDescriptor.configureOutput(binder)
        })
    }


    protected fun buildPagesAndReadInto(model: DocumentationNode, nodes: List<DocumentationNode>, sb: StringBuilder) =
        with(injector.getInstance(Generator::class.java)) {
            this as JavaLayoutHtmlFormatGenerator
            buildPages(listOf(model))
            val byLocations = nodes.groupBy { mainUri(it) }
            byLocations.forEach { (loc, _) ->
                sb.appendln("<!-- File: $loc -->")
                sb.append(folder.root.toURI().resolve(URI("/").relativize(loc)).toURL().readText())
            }
        }


    protected fun verifyNode(
        fileName: String,
        noStdlibLink: Boolean = false,
        fileExtension: String = ".html",
        select: (model: DocumentationNode) -> List<DocumentationNode>
    ) {
        verifyOutput(
            "testdata/format/java-layout-html/$fileName",
            fileExtension,
            format = "java-layout-html",
            withKotlinRuntime = true,
            noStdlibLink = noStdlibLink,
            collectInheritedExtensionsFromLibraries = true
        ) { model, output ->
            buildPagesAndReadInto(
                model,
                select(model),
                output
            )
        }
    }

    protected fun verifyNode(fileName: String, noStdlibLink: Boolean = false) {
        verifyNode(fileName, noStdlibLink) { model -> listOf(model.members.single().members.single()) }
    }

    protected fun verifyPackageNode(fileName: String, noStdlibLink: Boolean = false) {
        verifyOutput(
            "testdata/format/java-layout-html/$fileName",
            ".package-summary.html",
            format = "java-layout-html",
            withKotlinRuntime = true,
            noStdlibLink = noStdlibLink
        ) { model, output ->
            buildPagesAndReadInto(
                model,
                listOf(model.members.single()),
                output
            )
        }
    }
}