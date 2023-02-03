package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.dokka.DokkaConfigurationBuilder
import javax.inject.Inject

abstract class DokkaModuleDescriptionGradleBuilder @Inject constructor(
    providers: ProviderFactory,
) : DokkaConfigurationBuilder<DokkaConfigurationKxs.DokkaModuleDescriptionKxs>, Named {

    val moduleName: Provider<String> = providers.provider { name }

    abstract val sourceOutputDirectory: RegularFileProperty

    abstract val includes: ConfigurableFileCollection

    override fun build() =
        DokkaConfigurationKxs.DokkaModuleDescriptionKxs(
            moduleName = moduleName.get(),
            sourceOutputDirectory = sourceOutputDirectory.get().asFile,
            includes = includes.files,
        )
}
