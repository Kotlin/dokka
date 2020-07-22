package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.SourceLinkDefinitionImpl

class GradleSourceLinkBuilder(
    @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<SourceLinkDefinitionImpl> {
    @Input
    val path: Property<String> = project.objects.safeProperty<String>()
        .safeConvention("")

    @Input
    val url: Property<String> = project.objects.safeProperty<String>()
        .safeConvention("")

    @Optional
    @Input
    val lineSuffix: Property<String?> = project.objects.safeProperty()

    override fun build(): SourceLinkDefinitionImpl {
        return SourceLinkDefinitionImpl(
            path = path.getSafe(),
            url = url.getSafe(),
            lineSuffix = lineSuffix.getSafe()
        )
    }
}
