package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName

open class GFMOutputBuilder(
        to: StringBuilder,
        location: Location,
        generator: NodeLocationAwareGenerator,
        languageService: LanguageService,
        extension: String,
        impliedPlatforms: List<String>
) : MarkdownOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms) {
    override fun appendTable(vararg columns: String, body: () -> Unit) {
        to.appendln(columns.joinToString(" | ", "| ", " |"))
        to.appendln("|" + "---|".repeat(columns.size))
        body()
    }

    override fun appendUnorderedList(body: () -> Unit) {
        if (inTableCell) {
            wrapInTag("ul", body)
        } else {
            super.appendUnorderedList(body)
        }
    }

    override fun appendOrderedList(body: () -> Unit) {
        if (inTableCell) {
            wrapInTag("ol", body)
        } else {
            super.appendOrderedList(body)
        }
    }

    override fun appendListItem(body: () -> Unit) {
        if (inTableCell) {
            wrapInTag("li", body)
        } else {
            super.appendListItem(body)
        }
    }
}

open class GFMFormatService(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        linkExtension: String,
        impliedPlatforms: List<String>
) : MarkdownFormatService(generator, signatureGenerator, linkExtension, impliedPlatforms) {

    @Inject constructor(
            generator: NodeLocationAwareGenerator,
            signatureGenerator: LanguageService,
            @Named(impliedPlatformsName) impliedPlatforms: List<String>
    ) : this(generator, signatureGenerator, "md", impliedPlatforms)

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
            GFMOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
}
