package format

import Formats.multipleOption
import Formats.parseOptions
import Formats.singleOption
import com.google.inject.Guice
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.dokka.tests.appendDocumentation
import org.jetbrains.dokka.tests.tempLocation
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Mario Toffia
 */
class AsciidocFormatTest {
    @Test fun asciidocOptionsGetParsedCorrectly() {
        var options = """
								--base-dir the-base-dir
								--attribute
								"project_name=project-name"
								--attribute
								"project_version=1.0.2-SNAPSHOT"
								--attribute
								"project_desc=the-project-description"
								--attribute
								"imagesdir=asciidoctor.configuration.images-dir"
								--attribute
								"data-uri=asciidoctor.configuration.images-dir"
								--gem-path
								project.build.directory/gems-provided
								--require
								asciidoctor-diagram
"""

        val opts = parseOptions(options)

        assertEquals("the-base-dir", opts.singleOption("base-dir"))
        assertEquals(5, opts.multipleOption("attribute").size)
        assertEquals("asciidoctor.configuration.images-dir", opts.singleOption("attribute", "imagesdir"))
        assertEquals("asciidoctor.configuration.images-dir", opts.singleOption("attribute", "data-uri"))
        assertEquals("1.0.2-SNAPSHOT", opts.singleOption("attribute", "project_version"))
        assertEquals("asciidoctor-diagram", opts.singleOption("require"))
        assertEquals("", opts.singleOption("kalle-kobra"))
    }

    @Test fun asciiDocTableGetsRendered() {
        val str = generate("testdata/format/asciidocTable.kt")
        System.out.println(str)
    }

    @Test fun asciiDocJavaSourceGetsRendered() {
        val str = generate("testdata/format/asciidocJavaSource.kt")
        System.out.println(str)
    }

    private fun generate(file: String): String {
        val sb = StringBuilder()
        val cr = contentRootFromPath(file)
        val documentation = DocumentationModule("test")

        val options = DocumentationOptions("", "html-as-asciidoc",
                                           includeNonPublic = true,
                                           skipEmptyPackages = false,
                                           includeRootPackage = true,
                                           sourceLinks = listOf<DokkaConfiguration.SourceLinkDefinition>(),
                                           generateIndexPages = false,
                                           noStdlibLink = true,
                                           cacheRoot = "default")

        appendDocumentation(documentation, cr,
                            withJdk = false,
                            withKotlinRuntime = false,
                            options = options)

        val memb = documentation.members.single().members.single().members
        documentation.prepareForGeneration(options)

        val outputInjector = Guice.createInjector(DokkaOutputModule(options, DokkaConsoleLogger))
        outputInjector.getInstance(FormatService::class.java).createOutputBuilder(sb, tempLocation).appendNodes(memb);

        return sb.toString()
    }
}