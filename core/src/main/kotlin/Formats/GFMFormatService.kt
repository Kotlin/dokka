package org.jetbrains.dokka

import com.google.inject.Inject

open class GFMOutputBuilder(to: StringBuilder,
                                 location: Location,
                                 locationService: LocationService,
                                 languageService: LanguageService,
                                 extension: String)
    : MarkdownOutputBuilder(to, location, locationService, languageService, extension)
{
    override fun appendTable(vararg columns: String, body: () -> Unit) {
        to.appendln(columns.joinToString(" | ", "| ", " |"))
        to.appendln("|" + "---|".repeat(columns.size))
        body()
    }

    override fun appendUnorderedList(body: () -> Unit) {
        if (inTableCell) {
            wrapInTag("ul", body)
        }
        else {
            super.appendUnorderedList(body)
        }
    }

    override fun appendOrderedList(body: () -> Unit) {
        if (inTableCell) {
            wrapInTag("ol", body)
        }
        else {
            super.appendOrderedList(body)
        }
    }

    override fun appendListItem(body: () -> Unit) {
        if (inTableCell) {
            wrapInTag("li", body)
        }
        else {
            super.appendListItem(body)
        }
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
