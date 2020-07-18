package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import java.net.URL

class GradleExternalDocumentationLinkBuilder : DokkaConfigurationBuilder<ExternalDocumentationLinkImpl> {
    @Input
    var url: URL? = null

    @Input
    var packageListUrl: URL? = null

    override fun build(): ExternalDocumentationLinkImpl {
        return ExternalDocumentationLink(
            url = checkNotNull(url) { "url not specified " },
            packageListUrl = packageListUrl
        )
    }
}
