/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.shared

public enum class RecognizedLinkFormat(
    override val formatName: String,
    override val linkExtension: String
) : LinkFormat {
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

    public companion object {
        private val values = values()

        public fun fromString(formatName: String): RecognizedLinkFormat? {
            return values.firstOrNull { it.formatName == formatName }
        }
    }
}
