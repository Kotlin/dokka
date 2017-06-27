package format

import Formats.AsciidocFormatService
import org.jetbrains.dokka.*
import org.jetbrains.dokka.tests.InMemoryLocationService
import org.jetbrains.dokka.tests.appendDocumentation
import org.jetbrains.dokka.tests.tempLocation
import org.junit.Test

/**
 * @author Mario Toffia
 */
class AsciidocFormatTest {
  val asciidocService = AsciidocFormatService(InMemoryLocationService, KotlinLanguageService(), HtmlTemplateService
      .default(), listOf())

  @Test fun asciiDocTableGetsRendered() {
    val str = generate("testdata/format/asciidocTable.kt")
    if (null != str) {
      System.out.println(str)
    }
  }

  @Test fun asciiDocJavaSourceGetsRendered() {
    val str = generate("testdata/format/asciidocJavaSource.kt")
    if (null != str) {
      System.out.println(str)
    }
  }

  private fun generate(file : String) : String {
    val sb = StringBuilder()
    val cr = contentRootFromPath(file)
    val hs = asciidocService.createOutputBuilder(sb, tempLocation)

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
    hs.appendNodes(memb)

    return sb.toString()
  }
}