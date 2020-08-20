package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import java.net.URL

class GradleExternalDocumentationLinkBuilder(
    @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<ExternalDocumentationLinkImpl> {
    @Input
    val url: Property<URL?> = project.objects.safeProperty()

    @Optional
    @Input
    val packageListUrl: Property<URL?> = project.objects.safeProperty()

    override fun build(): ExternalDocumentationLinkImpl = ExternalDocumentationLink(
        url = checkNotNull(url.getSafe()) { "url not specified " },
        packageListUrl = packageListUrl.getSafe()
    )
}
