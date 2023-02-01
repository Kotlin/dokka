package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.DokkaSourceSetID

abstract class DokkaSourceSetIDGradleBuilder : DokkaConfigurationBuilder<DokkaSourceSetID>, Named {

    /**
     * Unique identifier of the scope that this source set is placed in.
     * Each scope provide only unique source set names.
     *
     * E.g. One DokkaTask inside the Gradle plugin represents one source set scope, since there cannot be multiple
     * source sets with the same name. However, a Gradle project will not be a proper scope, since there can be
     * multple DokkaTasks that contain source sets with the same name (but different configuration)
     */
    abstract val scopeId: Property<String>
    abstract val sourceSetName: Property<String>

    override fun getName(): String = sourceSetName.get()
}
