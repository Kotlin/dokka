package org.jetbrains.dokka.gradle.distibutions

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.kotlin.dsl.named
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaPluginSettings


/**
 * Initialise [DokkaPluginConfigurations].
 *
 * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
 * [DokkaConfiguration][org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs]
 * is used to configure Dokka behaviour.)
 */
internal fun Project.setupDokkaConfigurations(dokkaSettings: DokkaPluginSettings): DokkaPluginConfigurations {

    fun AttributeContainer.dokkaUsageAttribute() {
        attribute(Usage.USAGE_ATTRIBUTE, dokkaSettings.attributeValues.dokkaUsage)
    }

    fun AttributeContainer.dokkaConfigurationsAttributes() {
        dokkaUsageAttribute()
        attribute(Category.CATEGORY_ATTRIBUTE, dokkaSettings.attributeValues.dokkaConfigurationCategory)
    }

    fun AttributeContainer.dokkaModuleDescriptorAttributes() {
        dokkaUsageAttribute()
        attribute(Category.CATEGORY_ATTRIBUTE, dokkaSettings.attributeValues.dokkaModuleDescriptionCategory)
    }

    val dokkaConsumer =
        configurations.register(DokkaPlugin.CONFIGURATION_NAME__DOKKA) {
            description = "Fetch all Dokka files from all configurations in other subprojects"
            asConsumer()
            attributes { dokkaUsageAttribute() }
        }


    //<editor-fold desc="Dokka Configuration files">
    val dokkaConfigurationsConsumer =
        configurations.register(DokkaPlugin.CONFIGURATION_NAME__DOKKA_CONFIGURATIONS) {
            description = "Fetch Dokka Configuration files from other subprojects"
            asConsumer()
            attributes { dokkaConfigurationsAttributes() }
            extendsFrom(dokkaConsumer.get())
        }

    val dokkaConfigurationsProvider =
        configurations.register(DokkaPlugin.CONFIGURATION_NAME__DOKKA_CONFIGURATION_ELEMENTS) {
            description = "Provide Dokka Configurations files to other subprojects"
            asProvider()
            attributes { dokkaConfigurationsAttributes() }
        }
    //</editor-fold>


    //<editor-fold desc="Module descriptor configurations">
    val dokkaModuleDescriptorsConsumer =
        configurations.register(DokkaPlugin.CONFIGURATION_NAME__DOKKA_MODULE_DESCRIPTORS) {
            description = "Fetch Dokka module descriptor files from other subprojects"
            asConsumer()
            attributes { dokkaModuleDescriptorAttributes() }
            extendsFrom(dokkaConsumer.get())
        }

    val dokkaModuleDescriptorsProvider =
        configurations.register(DokkaPlugin.CONFIGURATION_NAME__DOKKA_MODULE_DESCRIPTOR_ELEMENTS) {
            description = "Provide Dokka module descriptor files to other subprojects"
            asProvider()
            attributes { dokkaModuleDescriptorAttributes() }
        }
    //</editor-fold>


    val dokkaRuntimeClasspath = configurations.register(DokkaPlugin.CONFIGURATION_NAME__DOKKA_RUNTIME_CLASSPATH) {
        description = "Dokka generation task runtime classpath"
        asConsumer()
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.STANDARD_JVM)
            )
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
        defaultDependencies {

            fun dokka(module: String) = addLater(
                dokkaSettings.dokkaVersion.map { version ->
                    project.dependencies.create("org.jetbrains.dokka:$module:$version")
                }
            )
            dokka("dokka-core")
            dokka("dokka-base")
            dokka("dokka-analysis")
            dokka("kotlin-analysis-compiler")
            dokka("kotlin-analysis-intellij")

            add(project.dependencies.create("org.jetbrains.kotlinx:kotlinx-html:0.8.0"))
        }
    }

    return DokkaPluginConfigurations(
        dokkaConsumer = dokkaConsumer,
        dokkaConfigurationsConsumer = dokkaConfigurationsConsumer,
        dokkaModuleDescriptorsConsumer = dokkaModuleDescriptorsConsumer,
        dokkaConfigurationsElements = dokkaConfigurationsProvider,
        dokkaModuleDescriptorsElements = dokkaModuleDescriptorsProvider,
        dokkaRuntimeClasspath = dokkaRuntimeClasspath,
    )
}

/** Mark this [Configuration] as one that will be consumed by other subprojects. */
private fun Configuration.asProvider() {
    isVisible = false
    isCanBeResolved = false
    isCanBeConsumed = true
}

/** Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving') */
private fun Configuration.asConsumer() {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
}
