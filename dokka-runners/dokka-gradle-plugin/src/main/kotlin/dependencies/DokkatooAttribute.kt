package org.jetbrains.dokka.gradle.dependencies

import org.jetbrains.dokka.gradle.internal.Attribute
import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute

/**
 * Gradle Configuration Attributes for sharing Dokkatoo files across subprojects.
 *
 * These attributes are used to tag [Configuration]s, so files can be shared between subprojects.
 */
@DokkatooInternalApi
interface DokkatooAttribute {

    /** HTML, Markdown, etc. */
    @DokkatooInternalApi
    @JvmInline
    value class Format(private val named: String) : Named {
        override fun getName(): String = named
    }

    /** Generated output, or subproject classpath, or included files, etc. */
    @DokkatooInternalApi
    @JvmInline
    value class ModuleComponent(private val named: String) : Named {
        override fun getName(): String = named
    }

    /** A classpath, e.g. for Dokka Plugins or the Dokka Generator. */
    @DokkatooInternalApi
    @JvmInline
    value class Classpath(private val named: String) : Named {
        override fun getName(): String = named
    }

    @DokkatooInternalApi
    companion object {
        val DokkatooFormatAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.format")

        val DokkatooModuleComponentAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.module-component")

        val DokkatooClasspathAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.classpath")
    }
}
