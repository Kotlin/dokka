package org.jetbrains.dokka

import com.google.inject.Inject

open class GFMOutputBuilder(to: StringBuilder,
                                 location: Location,
                                 locationService: LocationService,
                                 languageService: LanguageService,
                                 extension: String)
    : MarkdownOutputBuilder(to, location, locationService, languageService, extension)
{
    override fun appendTable(to: StringBuilder, vararg columns: String, body: () -> Unit) {
        to.appendln(columns.joinToString(" | ", "| ", " |"))
        to.appendln("|" + "---|".repeat(columns.size))
        body()
    }
}

open class GFMFormatService(locationService: LocationService,
                            signatureGenerator: LanguageService,
                            linkExtension: String)
: MarkdownFormatService(locationService, signatureGenerator, linkExtension) {

    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService) : this(locationService, signatureGenerator, "md")

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
        GFMOutputBuilder(to, location, locationService, languageService, extension)
}
