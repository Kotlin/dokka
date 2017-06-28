package Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import org.asciidoctor.*
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import org.jruby.ir.Tuple

private const val INLINE_DOCTYPE = "inline"

/**
 * Resource path to coderay stylesheet.
 */
const val CodeRayStyleSheet = "/dokka/styles/coderay-asciidoctor.css"

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
 * Options are stored in a map with a list of tuples. Single options do
 * have the tuple _a_ and _b_ properties set to the same value. If attributed
 * option i.e. it has _name=value_ it will use the _a_ property set to the
 * _name_ and _b_ property to the _value_.
 */
typealias FormatterOptions = Map<String, List<Tuple<String, String>>>

/**
 * Adds attributes from the `FormatterOptions` into existing builder.
 */
private fun AttributesBuilder.addFromOptions(options: FormatterOptions) : AttributesBuilder {
    for(attr in options.multipleOption("attribute")) {
      this.attribute(attr.a,attr.b)
    }
    return this;
}

/**
 * Parses a string into a `FormatterOptions`.
 */
fun parseOptions(options : String) : FormatterOptions {
  fun trimString(s : String) : String = s.trim('\"',' ','\n','\r','\t')

  fun createTuple(s : String) : Tuple<String,String> {
    val idx = s.indexOf('=');
    return Tuple(trimString(s.substring(0, idx)), trimString(s.substring(idx + 1)))
  }

  fun getOption(s : String) : Tuple<String, String> {
    var idx = s.indexOf(' ');
    if (-1 == idx) {
      idx = s.indexOf('\n')
      if (-1 == idx) {
        return Tuple(trimString(s), "");
      }
    }
    return Tuple(trimString(s.substring(0, idx)), trimString(s.substring(idx + 1)))
  }

  val map = mutableMapOf<String, List<Tuple<String, String>>>();

  val args = options.split("--")
  for(s in args) {
    val trimmed = trimString(s)
    if (trimmed.isNullOrBlank()) {
      continue
    }

    val option = getOption(trimmed)
    val list = map.getOrPut(option.a,{ mutableListOf()}) as MutableList<Tuple<String,String>>

    when {
      "=" in option.b -> list.add(createTuple(option.b))
      else -> list.add(Tuple(option.b,trimString(option.b)))
    }
  }

  return map;
}

/**
 * Extracts a single option value based on the option name and _Optionally_ a
 * name (as the arrKey) if the option is _name=value_ based. If not found
 * an empty `String` is returned.
 */
fun FormatterOptions.singleOption(key : String, arrKey : String? = null) : String {
  val opts = this.get(key)
  if (null == opts) {
    return ""
  }

  if (null == arrKey) {
    return opts.stream().findFirst().orElse(Tuple("not-found", "")).b
  }

  return opts.stream().filter { it.a == arrKey }.findFirst().orElse(Tuple("not-found","")).b
}

/**
 * Gets a list of option values if any. If not found a empty list is returned.
 */
fun FormatterOptions.multipleOption(key : String) : List<Tuple<String,String>> = this.get(key) ?: emptyList()

/**
 * Renders using `Asciidoctor` instead of plain _HTML_ markup.
 *
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
    to.append(render(text, false))
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
      to.append(render(sb.toString(), false))
      sb = StringBuilder()
    }
  }

  /**
   * Renders the input using Asciidoctor.
   *
   * The source is first cleaned by stripping any trailing space after an
   * end line (e.g., `"\n "`), which gets left behind by the Javadoc
   * processor.
   *
   * @param input AsciiDoc source
   *
   * @return content rendered by Asciidoctor
   */
  private fun render(input: String, inline: Boolean): String {
    if (input.trim().isEmpty()) {
      return ""
    }

    options.setDocType(if (inline) INLINE_DOCTYPE else null)
    return asciidoctor.render(cleanJavadocInput(input), options)
  }

  private fun cleanJavadocInput(input: String): String {
    return input.trim { it <= ' ' }
        .replace("\n ".toRegex(), "\n") // Newline space to accommodate javadoc newlines.
        .replace("\\{at}".toRegex(), "&#64;") // {at} is translated into @.
        .replace("\\{slash}".toRegex(), "/") // {slash} is translated into /.
        .replace("(?m)^( *)\\*\\\\/$".toRegex(), "$1*/") // Multi-line comment end tag is translated into */.
        .replace("\\{@literal (.*?)}".toRegex(), "$1") // {@literal _} is translated into _ (standard javadoc).
  }

}

/**
 * `Asciidoctor` format service of which instantiates a `Asciidoctor` instance
 * and reuses it for each invocation to `createOutputBuilder(StringBuilder, Location)`.
 */
class AsciidocFormatService @Inject constructor(@Named("folders") locationService: LocationService,
                                                 signatureGenerator: LanguageService,
                                                 templateService: HtmlTemplateService,
                                                 @Named(impliedPlatformsName) impliedPlatforms: List<String>)
  :HtmlFormatService(locationService,signatureGenerator, templateService, impliedPlatforms) {

      @set:Inject(optional = true) lateinit var docOptions: DocumentationOptions

      val formatterOptions : FormatterOptions by lazy {
        parseOptions(docOptions.additionalParams ?: "")
      }

      val options : Options by lazy {
        defaultOptions().attributes(defaultAttributes().addFromOptions(formatterOptions)).get()
      }

      val asciidoctor : Asciidoctor by lazy {
        Asciidoctor.Factory.create(formatterOptions.singleOption("gem-path"))
      }

      override fun enumerateSupportFiles(callback: (String, String) -> Unit) {
        callback(selectStylesheet(), "style.css")
        callback(CodeRayStyleSheet,"coderay-asciidoctor.css")
      }

      override fun createOutputBuilder(to: StringBuilder, location: Location) =
          AsciidocOutputBuilder(to, location, locationService, languageService, extension, impliedPlatforms,
              templateService, asciidoctor, options)
}