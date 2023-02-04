package org.jetbrains.dokka.gradle.distibutions

import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import javax.inject.Inject

abstract class DokkaPluginAttributes @Inject constructor(
    private val objects: ObjectFactory,
) {

    /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
//    val dokkaBaseUsage: Usage = objects.named("org.jetbrains.dokka")
    val dokkaBaseUsage: DokkaBase get() = objects.named("dokka")

    /** for [Configuration]s that provide or consume Dokka configuration files */
    val dokkaConfiguration: DokkaCategory get() = objects.named("configuration")

    /** for [Configuration]s that provide or consume Dokka module descriptor files */
    val dokkaModuleDescriptors: DokkaCategory get() = objects.named("module-descriptor")

    val dokkaGeneratorClasspath: DokkaCategory get() = objects.named("generator-classpath")

    val dokkaPluginsClasspath: DokkaCategory get() = objects.named("plugins-classpath")

    interface DokkaBase : Usage

    interface DokkaCategory : Named

    companion object {
        val DOKKA_BASE_ATTRIBUTE = Attribute.of("org.jetbrains.dokka.base", DokkaBase::class.java)
        val DOKKA_CATEGORY_ATTRIBUTE = Attribute.of("org.jetbrains.dokka.category", DokkaCategory::class.java)
    }

}