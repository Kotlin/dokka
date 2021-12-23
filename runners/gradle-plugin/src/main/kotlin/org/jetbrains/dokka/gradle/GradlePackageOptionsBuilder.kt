@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.setProperty
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl


class GradlePackageOptionsBuilder(
    @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<PackageOptionsImpl> {
    @Input
    val matchingRegex: Property<String> = project.objects.safeProperty<String>()
        .safeConvention(".*")

    @Input
    val includeNonPublic: Property<Boolean> = project.objects.safeProperty<Boolean>()
        .safeConvention(DokkaDefaults.includeNonPublic)

    @Input
    val documentedVisibilities: SetProperty<DokkaConfiguration.Visibility> = project.objects.setProperty<DokkaConfiguration.Visibility>()
        .convention(DokkaDefaults.documentedVisibilities)

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
        matchingRegex = checkNotNull(matchingRegex.getSafe()) { "prefix not specified" },
        includeNonPublic = includeNonPublic.getSafe(),
        documentedVisibilities = documentedVisibilities.getSafe(),
        reportUndocumented = reportUndocumented.getSafe(),
        skipDeprecated = skipDeprecated.getSafe(),
        suppress = suppress.getSafe()
    )
}
