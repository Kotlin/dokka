package org.jetbrains.dokka.base.resolvers.shared

enum class RecognizedLinkFormat(override val formatName: String, override val linkExtension: String) : LinkFormat {
    DokkaHtml("html-v1", "html"),
    DokkaJavadoc("javadoc-v1", "html"),
    DokkaGFM("gfm-v1", "md"),
    DokkaJekyll("jekyll-v1", "html"),
    Javadoc1("javadoc1", "html"),
    Javadoc8("javadoc8", "html"),
    Javadoc10("javadoc10", "html"),
    DokkaOldHtml("html", "html"),
    KotlinWebsite("kotlin-website", "html"),
    KotlinWebsiteHtml("kotlin-website-html", "html");

    companion object {
        fun fromString(formatName: String) =
            values().firstOrNull { it.formatName == formatName }
    }
}
