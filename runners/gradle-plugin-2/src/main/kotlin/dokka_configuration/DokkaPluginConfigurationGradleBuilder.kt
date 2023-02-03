package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationBuilder

abstract class DokkaPluginConfigurationGradleBuilder :
    DokkaConfigurationBuilder<DokkaConfigurationKxs.PluginConfigurationKxs> {

    @get:Input
    abstract val fqPluginName: Property<String>

    @get:Input
    abstract val serializationFormat: Property<DokkaConfiguration.SerializationFormat>

    @get:Input
    abstract val values: Property<String>

    override fun build() =
        DokkaConfigurationKxs.PluginConfigurationKxs(
            fqPluginName = fqPluginName.get(),
            serializationFormat = serializationFormat.get(),
            values = values.get(),
        )
}
