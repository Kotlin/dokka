package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import java.io.File
import java.net.URL

class GradleSourceLinkBuilder(
    @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<SourceLinkDefinitionImpl> {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    val localDirectory: Property<File?> = project.objects.safeProperty()

    @Input
    val remoteUrl: Property<URL?> = project.objects.safeProperty<URL>()

    @Optional
    @Input
    val remoteLineSuffix: Property<String> = project.objects.safeProperty<String>()
        .safeConvention("#L")

    override fun build(): SourceLinkDefinitionImpl {
        return SourceLinkDefinitionImpl(
            localDirectory = localDirectory.getSafe()?.canonicalPath ?: project.projectDir.canonicalPath,
            remoteUrl = checkNotNull(remoteUrl.getSafe()) { "missing remoteUrl on source link" },
            remoteLineSuffix = remoteLineSuffix.getSafe()
        )
    }
}
