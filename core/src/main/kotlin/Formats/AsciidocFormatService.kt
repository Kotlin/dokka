package Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import org.asciidoctor.SafeMode
import org.asciidoctor.OptionsBuilder
import org.asciidoctor.AttributesBuilder


/**
 * Selects the correct stylesheet depending on the
 * _java.version_ system property.
 */
fun selectStylesheet() : String {
  val version = System.getProperty("java.version") ?: "";
  if (version.matches("^1\\.[56]\\D.*".toRegex()))
    return "/dokka/styles/stylesheet6.css"
  if (version.matches("^1\\.[78]\\D.*".toRegex()))
    return "/dokka/styles/stylesheet8.css"

  return "/dokka/styles/stylesheet8.css"
}

const val CodeRayStyleSheet = "/dokka/styles/coderay-asciidoctor.css"


private fun defaultOptions(): OptionsBuilder {
  return OptionsBuilder.options()
      .safe(SafeMode.SAFE)
      .backend("html5")
}

private fun defaultAttributes(): AttributesBuilder {

  return AttributesBuilder.attributes()
      .attribute("at", "&#64;")
      .attribute("slash", "/")
      .attribute("icons", null)
      .attribute("idprefix", "")
      .attribute("idseparator", "-")
      .attribute("javadoc", "")
      .attribute("showtitle", true)
      .attribute("source-highlighter", "coderay")
      .attribute("coderay-css", "class")
      .attribute("env-asciidoclet")
      .attribute("env", "asciidoclet")
}

/**
 * @author Mario Toffia
 */
class AsciidocOutputBuilder(to: StringBuilder,
                            location: Location,
                            locationService: LocationService,
                            languageService: LanguageService,
                            extension: String,
                            impliedPlatforms: List<String>,
                            templateService: HtmlTemplateService,
                            private val asciidoctor: Asciidoctor,
                            private val options : Options)
  : HtmlOutputBuilder(to, location, locationService, languageService, extension, impliedPlatforms, templateService)
{
  var sb = StringBuilder()

  override fun appendText(text: String) {
    to.append(asciidoctor.render(text, options))
  }

  override fun appendContent(content: List<ContentNode>) {
    super.appendContent(content)
    flushBuffer()
  }

  override fun appendContent(content: ContentNode) {
    when(content) {
      is ContentText -> sb.append(content.text)
      is ContentSymbol -> sb.append(content.text)
      is ContentKeyword -> sb.append(content.text)
      is ContentIdentifier -> sb.append(content.text)
      is ContentEntity -> sb.append(content.text)
      else -> {
        flushBuffer()
        super.appendContent(content)
      }
    }
  }

  private fun flushBuffer() {
    if (sb.length > 0) {
      to.append(asciidoctor.render(sb.toString(), options))
      sb = StringBuilder()
    }
  }
}

class AsciidocFormatService @Inject constructor(@Named("folders") locationService: LocationService,
                                                 signatureGenerator: LanguageService,
                                                 templateService: HtmlTemplateService,
                                                 @Named(impliedPlatformsName) impliedPlatforms: List<String>)
  :HtmlFormatService(locationService,signatureGenerator, templateService, impliedPlatforms) {

  val options : Options = defaultOptions().attributes(defaultAttributes().get()).get()
  val asciidoctor : Asciidoctor = Asciidoctor.Factory.create()

  override fun enumerateSupportFiles(callback: (String, String) -> Unit) {
    callback(selectStylesheet(), "style.css")
  }

  override fun createOutputBuilder(to: StringBuilder, location: Location) =
      AsciidocOutputBuilder(to, location, locationService, languageService, extension, impliedPlatforms,
          templateService, asciidoctor, options)
}