package javadoc

import com.google.inject.Guice
import com.intellij.util.io.delete
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.dokka.tests.appendDocumentation
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue

/**
 * @author Mario Toffia
 */
class JavaAsciidocTest {
  @Test fun javaAsciidocJavaSourceGetsRendered() {
    val str = generate("testdata/format/asciidocJavaSource.kt","AsciidocJavaSourceKt.html")

    assertTrue { str.contains("<pre class=\"CodeRay highlight\"><code data-lang=\"java\"><span " +
        "class=\"directive\">public</span>" +
        " <span class=\"type\">class</span> <span class=\"class\">SvenneSvensson</span>") }
  }

  @Test fun javaAsciidocTableGetsRendered() {
    val str = generate("testdata/format/asciidocTable.kt","AsciidocTableKt.html")

    assertTrue { str.contains("<table class=\"tableblock frame-all grid-all spread\">") }
    assertTrue {str.contains("<col style=\"width: 50%;\">") }
    assertTrue {str.contains("<td class=\"tableblock halign-left valign-top\"><p " +
        "class=\"tableblock\">Firefox</p></td>") }
  }

  @Test fun javaAsciidocAttributesGetsPassed() {
    val str = generate("testdata/format/asciidocTable.kt","AsciidocTableKt.html")

    assertTrue { str.contains("<table class=\"tableblock frame-all grid-all spread\">") }
    assertTrue {str.contains("<col style=\"width: 50%;\">") }
    assertTrue {str.contains("<td class=\"tableblock halign-left valign-top\"><p " +
        "class=\"tableblock\">Firefox</p></td>") }
  }

  @Test fun javaAsciidocLinksIsRenderedCorrectly() {
    val str = generate("testdata/format/asciidocLinks.kt","AsciidocLinksKt.html")
    println(str)
  }

  @Test fun javaAsciidocParameterAttributesFormattedCorrectly() {
    val str = generate("testdata/format/asciidocAttributes.kt","AsciidocAttributesKt.html")
    assertTrue { str.contains("The project name") }
    assertTrue { str.contains("A project description") }
    assertTrue { str.contains("1.5.2-SNAPSHOT") }
  }

  private fun generate(file : String, htmlFile : String) : String {
    val cr = contentRootFromPath(file)
    val documentation = DocumentationModule("test")
    val tmpDir = Files.createTempDirectory("test-")

    val options = DocumentationOptions(tmpDir.toString(), "java-asciidoc",
        includeNonPublic = true,
        skipEmptyPackages = false,
        includeRootPackage = true,
        sourceLinks = listOf<DokkaConfiguration.SourceLinkDefinition>(),
        generateIndexPages = false,
        noStdlibLink = true,
        cacheRoot = "default",
        additionalParams = """
        --attribute project_name=The project name
        --attribute project_desc="A project description"
        --attribute project_version=1.5.2-SNAPSHOT
        """)

    appendDocumentation(documentation, cr,
        withJdk = false,
        withKotlinRuntime = false,
        options = options)

    documentation.prepareForGeneration(options)

    val outputInjector = Guice.createInjector(DokkaOutputModule(options, DokkaConsoleLogger))
    outputInjector.getInstance(Generator::class.java).buildAll(documentation)

    val str = File(tmpDir.toString(), htmlFile).readText()
    tmpDir.delete()
    return str
  }
}
