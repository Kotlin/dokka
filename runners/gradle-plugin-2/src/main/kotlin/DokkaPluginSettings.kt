package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.domainObjectContainer
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaModuleDescriptionGradleBuilder
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaSourceSetGradleBuilder
import javax.inject.Inject

/**
 * Configure the behaviour of the [DokkaPlugin].
 */
abstract class DokkaPluginSettings @Inject constructor(
    private val objects: ObjectFactory
) {
    /** Default version used for Dokka dependencies */
    abstract val dokkaVersion: Property<String>

    /** Default Dokka cache directory */
    abstract val dokkaCacheDirectory: DirectoryProperty

    /**
     * Dokka Source Sets describe the source code that should be included in a Dokka Publication.
     *
     * Dokka will not generate documentation at least there is at least one Dokka Source Set.
     *
     * Only source sets that are contained within _this project_ should be included here.
     * To merge source sets from other projects, use the Gradle dependencies block.
     *
     * ```kotlin
     * dependencies {
     *   // merge :other-project into this project's Dokka Configuration
     *   dokka(project(":other-project"))
     * }
     * ```
     *
     * Or, to include other Dokka Publications as a Module use
     *
     * ```kotlin
     * dependencies {
     *   // include :other-project as a module in this project's Dokka Configuration
     *   dokkaModule(project(":other-project"))
     * }
     * ```
     *
     * Dokka will merge Dokka Source Sets from other subprojects.
     */
    val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder> =
        objects.domainObjectContainer(DokkaSourceSetGradleBuilder::class)

    /**
     * Dokka Module Descriptions describe an independent Dokka publication, and these
     * descriptions are used by _other_ Dokka Configurations.
     *
     * Only add a module if you want the Dokka Publication produced by _this project_ to be
     * included in the Dokka Publication of _another_ project.
     */
    val dokkaModules: NamedDomainObjectContainer<DokkaModuleDescriptionGradleBuilder> =
        objects.domainObjectContainer(DokkaModuleDescriptionGradleBuilder::class)

}
