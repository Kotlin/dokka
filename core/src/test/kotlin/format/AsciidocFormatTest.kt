package org.jetbrains.dokka.tests.format

import com.google.inject.Guice
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.dokka.tests.appendDocumentation
import org.jetbrains.dokka.tests.tempLocation
import org.junit.Test
import kotlin.test.assertTrue

/**
 * @author Mario Toffia
 */
class AsciidocFormatTest {
    @Test fun asciiDocTableGetsRendered() {
        val str = generate("testdata/format/asciidocTable.kt")

        assertTrue { str.contains("<table class=\"tableblock frame-all grid-all spread\">") }
        assertTrue { str.contains("<col style=\"width: 50%;\">") }
        assertTrue {
            str.contains("<td class=\"tableblock halign-left valign-top\"><p " +
                         "class=\"tableblock\">Firefox</p></td>")
        }
    }

    @Test fun asciiDocJavaSourceGetsRendered() {
        val str = generate("testdata/format/asciidocJavaSource.kt")
        assertTrue {
            str.contains("<pre class=\"CodeRay highlight\"><code data-lang=\"java\"><span " +
                         "class=\"directive\">public</span>" +
                         " <span class=\"type\">class</span> <span class=\"class\">SvenneSvensson</span>")
        }
    }

    private fun generate(file: String): String {
        val sb = StringBuilder()
        val cr = contentRootFromPath(file)
        val documentation = DocumentationModule("test")

        val options = DocumentationOptions("", "html-as-asciidoc",
                                           includeNonPublic = true,
                                           skipEmptyPackages = false,
                                           includeRootPackage = true,
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
        outputInjector.getInstance(FormatService::class.java).createOutputBuilder(sb, tempLocation).appendNodes(memb)

        return sb.toString()
    }
}