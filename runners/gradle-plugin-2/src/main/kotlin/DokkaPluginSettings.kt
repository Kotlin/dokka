package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.named
import javax.inject.Inject

/**
 * Configure the behaviour of the [DokkaPlugin].
 */
abstract class DokkaPluginSettings @Inject constructor(
    private val objects: ObjectFactory
) {
    abstract val dokkaVersion: Property<String>
    abstract val dokkaWorkDir: RegularFileProperty

    // TODO use these properties to make relative paths in the Dokka Module config
    abstract val rootWorkDirectory: RegularFileProperty
    abstract val rootSourceDirectory: RegularFileProperty
    abstract val rootCacheDirectory: RegularFileProperty

    val attributeValues = DokkaAttributeValues()

    inner class DokkaAttributeValues {

        /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
        val dokkaUsage: Usage = objects.named("org.jetbrains.dokka")

        /** for [Configuration]s that provide or consume Dokka configuration files */
        val dokkaConfigurationCategory: Category = objects.named("configuration")

        /** for [Configuration]s that provide or consume Dokka module descriptor files */
        val dokkaModuleDescriptionCategory: Category = objects.named("module-descriptor")
    }
}
