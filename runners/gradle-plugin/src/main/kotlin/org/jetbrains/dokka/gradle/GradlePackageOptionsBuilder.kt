@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl


class GradlePackageOptionsBuilder(
    @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<PackageOptionsImpl> {
    @Input
    val prefix: Property<String> = project.objects.safeProperty<String>()
        .safeConvention("")

    @Input
    val includeNonPublic: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.includeNonPublic)

    @Input
    val reportUndocumented: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.reportUndocumented)

    @Input
    val skipDeprecated: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.skipDeprecated)

    @Input
    val suppress: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.suppress)

    override fun build(): PackageOptionsImpl = PackageOptionsImpl(
        prefix = checkNotNull(prefix.getSafe()) { "prefix not specified" },
        includeNonPublic = includeNonPublic.getSafe(),
        reportUndocumented = reportUndocumented.getSafe(),
        skipDeprecated = skipDeprecated.getSafe(),
        suppress = suppress.getSafe()
    )
}
