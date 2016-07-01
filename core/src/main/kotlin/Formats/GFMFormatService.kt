package org.jetbrains.dokka

import com.google.inject.Inject

open class GFMFormatService(locationService: LocationService,
                            signatureGenerator: LanguageService,
                            linkExtension: String)
: MarkdownFormatService(locationService, signatureGenerator, linkExtension) {

    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService) : this(locationService, signatureGenerator, "md")

    override fun appendTable(to: StringBuilder, vararg columns: String, body: () -> Unit) {
        to.appendln(columns.joinToString(" | ", "| ", " "))
        to.appendln("|" + "---|".repeat(columns.size))
        body()
    }
}
